-- ============================================================
-- Migracion: 2026-11-04_cierre_inscripciones_grupo.sql
-- Cierre de inscripciones obligatorio y cierre manual por grupo (Fase 3 del modulo de inscripciones).
--
-- Que hace:
--   1) tbl_grupo.GRP_INSCRIP_CERRADA_MANUAL TINYINT(1) NOT NULL DEFAULT 0: el Coordinador puede cerrar a mano las
--      inscripciones de un grupo antes de su fecha de cierre (1 = cerrado a mano).
--   2) Rellena GRP_FECHA_INSCRIP_CIERRE en los grupos que la tienen vacia, con la fecha de finalizacion del grupo
--      (GRP_FECHA_FINALIZACION). Desde esta fase toda fecha de cierre es obligatoria: un grupo sin ella quedaba
--      abierto para siempre.
--
-- Estado de inscripciones del curso: ya NO se guarda ni se cambia a mano; el backend lo calcula con los grupos del
-- curso. La columna tbl_curso.CUR_ESTADO_INSCRIPCIONES queda sin uso (no se borra en esta migracion).
--
-- Datos existentes:
--   * Los grupos con fecha de cierre no se tocan.
--   * Los grupos sin cierre y CON fecha de finalizacion reciben esa fecha como cierre. Si la finalizacion es anterior
--     a la apertura de inscripciones se omite el relleno (el cierre no puede ser anterior a la apertura).
--   * Los grupos sin cierre y SIN finalizacion quedan como estaban: el listado final los muestra para que el
--     Coordinador les ponga una fecha. Hasta entonces siguen abiertos; editarlos exigira la fecha de cierre.
--   * Los grupos eliminados logicamente (META_ELIMINADO = 1) no se modifican.
--
-- Quien lo ejecuta: una persona, manualmente, una vez por base de datos (ddl-auto=none).
-- Ejemplo: mysql --default-character-set=utf8mb4 -h<host> -P<puerto> -u<usuario> -p <basededatos> < db/migraciones/2026-11-04_cierre_inscripciones_grupo.sql
--
-- IDEMPOTENTE: la columna se verifica en INFORMATION_SCHEMA antes de crearse y el relleno solo toca filas con el
-- cierre vacio; correrlo dos veces no falla ni cambia nada la segunda vez.
-- ============================================================

-- 1) tbl_grupo.GRP_INSCRIP_CERRADA_MANUAL
SET @columna_existe = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_grupo'
      AND COLUMN_NAME = 'GRP_INSCRIP_CERRADA_MANUAL'
);
SET @sql_alter = IF(
    @columna_existe = 0,
    'ALTER TABLE tbl_grupo ADD COLUMN GRP_INSCRIP_CERRADA_MANUAL TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt_alter FROM @sql_alter;
EXECUTE stmt_alter;
DEALLOCATE PREPARE stmt_alter;

-- 2) Relleno de cierres vacios con la fecha de finalizacion. El WHERE filtra por fechas, no por clave: Workbench en
--    "safe update mode" lo rechaza (error 1175), asi que se desactiva solo para esta sesion y solo durante este UPDATE.
SET @safe_updates_anterior = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

UPDATE tbl_grupo
SET GRP_FECHA_INSCRIP_CIERRE = GRP_FECHA_FINALIZACION
WHERE META_ELIMINADO = 0
  AND GRP_FECHA_INSCRIP_CIERRE IS NULL
  AND GRP_FECHA_FINALIZACION IS NOT NULL
  AND (GRP_FECHA_INSCRIP_APERTURA IS NULL OR GRP_FECHA_FINALIZACION >= GRP_FECHA_INSCRIP_APERTURA);

SET SQL_SAFE_UPDATES = @safe_updates_anterior;

-- 3) Pendientes: grupos activos que siguen sin fecha de cierre (el Coordinador debe asignarla).
SELECT CAT_TITULO AS categoria, CUR_NOMBRE AS curso, GRP_ANIO AS anio, GRP_ITERABLE AS grupo,
       GRP_FECHA_INSCRIP_APERTURA AS apertura, GRP_FECHA_FINALIZACION AS finalizacion
FROM tbl_grupo
WHERE META_ELIMINADO = 0
  AND GRP_FECHA_INSCRIP_CIERRE IS NULL
ORDER BY CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE;
