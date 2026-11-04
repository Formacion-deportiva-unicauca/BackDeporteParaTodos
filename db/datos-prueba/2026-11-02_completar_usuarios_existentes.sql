-- ============================================================
-- DATOS DE PRUEBA -- NO ejecutar en produccion.
-- Completa los usuarios que ya estaban registrados antes de 2026-11-02_perfil_por_tipo_vinculo.sql, para que tengan los
-- datos que ahora pide el registro segun su tipo de vinculo:
--   * Estudiante y Docente: un programa academico (la facultad se deriva de el). Se les asigna 'Ingeniería Civil'.
--   * Administrativo y Contratista: una dependencia. Se les asigna 'Dependencia de prueba'.
-- No toca Instructores ni Coordinadores (su registro no pide estos datos) ni a quien ya tenga el dato.
--
-- Requisito: haber ejecutado antes db/migraciones/2026-11-02_perfil_por_tipo_vinculo.sql (columna ALM_DEPENDENCIA y
-- catalogo de programas, que incluye 'Ingeniería Civil').
--
-- IDEMPOTENTE: una segunda corrida no encuentra filas por completar y no cambia nada.
-- Despues de ejecutarlo, los usuarios deben cerrar sesion y volver a entrar para ver los datos en "Tu información".
-- Ejemplo: mysql --default-character-set=utf8mb4 -h<host> -P<puerto> -u<usuario> -p <basededatos> < db/datos-prueba/2026-11-02_completar_usuarios_existentes.sql
-- ============================================================

-- Vista previa (opcional): quienes se van a completar.
SELECT a.PERF_ID, p.PERF_NOMBRE, a.ALM_TIPO,
       CASE WHEN a.ALM_TIPO IN ('Estudiante', 'Docente') THEN 'recibira programa' ELSE 'recibira dependencia' END AS accion
FROM tbl_alumno a
JOIN tbl_perfil p ON p.PERF_ID = a.PERF_ID
WHERE a.META_ELIMINADO = 0
  AND a.PERF_ID NOT IN (SELECT PERF_ID FROM tbl_instructor)
  AND a.PERF_ID NOT IN (SELECT PERF_ID FROM tbl_coordinador)
  AND (
        (a.ALM_TIPO IN ('Estudiante', 'Docente')
         AND NOT EXISTS (SELECT 1 FROM tbl_intermedia_alumno_programa i WHERE i.PERF_ID = a.PERF_ID))
     OR (a.ALM_TIPO IN ('Administrativo', 'Contratista')
         AND (a.ALM_DEPENDENCIA IS NULL OR a.ALM_DEPENDENCIA = ''))
      );

-- 1) Estudiantes y Docentes sin programa.
INSERT IGNORE INTO tbl_intermedia_alumno_programa (META_ELIMINADO, PERF_ID, PRG_NOMBRE)
SELECT 0, a.PERF_ID, 'Ingeniería Civil'
FROM tbl_alumno a
WHERE a.META_ELIMINADO = 0
  AND a.ALM_TIPO IN ('Estudiante', 'Docente')
  AND a.PERF_ID NOT IN (SELECT PERF_ID FROM tbl_instructor)
  AND a.PERF_ID NOT IN (SELECT PERF_ID FROM tbl_coordinador)
  AND NOT EXISTS (SELECT 1 FROM tbl_intermedia_alumno_programa i WHERE i.PERF_ID = a.PERF_ID);

-- 2) Administrativos y Contratistas sin dependencia.
-- MySQL Workbench trae el modo seguro activado (error 1175: UPDATE sin WHERE sobre columna clave). Se desactiva
-- SOLO para esta sesion y se restaura al final.
SET @safe_updates_previo = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

UPDATE tbl_alumno
SET ALM_DEPENDENCIA = 'Dependencia de prueba'
WHERE META_ELIMINADO = 0
  AND ALM_TIPO IN ('Administrativo', 'Contratista')
  AND (ALM_DEPENDENCIA IS NULL OR ALM_DEPENDENCIA = '')
  AND PERF_ID NOT IN (SELECT PERF_ID FROM tbl_instructor)
  AND PERF_ID NOT IN (SELECT PERF_ID FROM tbl_coordinador);

SET SQL_SAFE_UPDATES = @safe_updates_previo;

-- Comprobacion (opcional): ningun alumno por completar.
SELECT a.PERF_ID, a.ALM_TIPO
FROM tbl_alumno a
WHERE a.META_ELIMINADO = 0
  AND a.PERF_ID NOT IN (SELECT PERF_ID FROM tbl_instructor)
  AND a.PERF_ID NOT IN (SELECT PERF_ID FROM tbl_coordinador)
  AND (
        (a.ALM_TIPO IN ('Estudiante', 'Docente')
         AND NOT EXISTS (SELECT 1 FROM tbl_intermedia_alumno_programa i WHERE i.PERF_ID = a.PERF_ID))
     OR (a.ALM_TIPO IN ('Administrativo', 'Contratista')
         AND (a.ALM_DEPENDENCIA IS NULL OR a.ALM_DEPENDENCIA = ''))
      );
