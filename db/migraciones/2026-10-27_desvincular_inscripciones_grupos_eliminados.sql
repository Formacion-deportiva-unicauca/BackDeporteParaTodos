-- ============================================================
-- Migracion: 2026-10-27_desvincular_inscripciones_grupos_eliminados.sql
-- Limpieza unica de datos historicos.
--
-- Hasta ahora DELETE /grupo marcaba el grupo como eliminado (META_ELIMINADO=1)
-- pero NO desvinculaba sus inscripciones. Esas filas siguen activas
-- (FECHADESVINCULACION IS NULL), le consumen al alumno uno de sus cupos de
-- curso y aparecen en "Mis cursos" como "Este grupo ya no esta disponible".
-- A partir de este cambio el backend las desvincula al eliminar el grupo;
-- esta migracion hace lo mismo con los grupos eliminados ANTES del cambio.
--
-- Efecto: solo rellena FECHADESVINCULACION (con la fecha actual) en las
-- inscripciones activas de grupos eliminados. No toca INSCR_ESTADO (queda como
-- registro historico) ni META_ELIMINADO, y no promueve lista de espera.
--
-- IDEMPOTENTE: una segunda corrida no encuentra filas con
-- FECHADESVINCULACION IS NULL en grupos eliminados y no cambia nada.
--
-- Quien lo ejecuta: una persona, manualmente, una vez por base de datos
-- (ddl-auto=none). Ejemplo:
--   mysql -h<host> -P<puerto> -u<usuario> -p <basededatos> < db/migraciones/2026-10-27_desvincular_inscripciones_grupos_eliminados.sql
-- ============================================================

-- MySQL Workbench trae el modo seguro activado (error 1175: UPDATE sin WHERE sobre
-- columna clave). Se desactiva SOLO para esta sesion y se restaura al final.
SET @safe_updates_previo = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

UPDATE tbl_inscripcion i
JOIN tbl_grupo g
  ON g.CAT_TITULO = i.CAT_TITULO
 AND g.CUR_NOMBRE = i.CUR_NOMBRE
 AND g.GRP_ANIO = i.GRP_ANIO
 AND g.GRP_ITERABLE = i.GRP_ITERABLE
SET i.INSCR_FECHADESVINCULACION = NOW()
WHERE g.META_ELIMINADO = 1
  AND i.META_ELIMINADO = 0
  AND i.INSCR_FECHADESVINCULACION IS NULL;

SET SQL_SAFE_UPDATES = @safe_updates_previo;
