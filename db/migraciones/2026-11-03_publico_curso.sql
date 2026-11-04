-- ============================================================
-- Migracion: 2026-11-03_publico_curso.sql
-- Publico del curso (Fase 2 del modulo de inscripciones).
--
-- Que hace:
--   tbl_curso.CUR_PUBLICO VARCHAR(15) NOT NULL DEFAULT 'MIXTO', con CHECK (CKC_CURSO_PUBLICO) para
--   'ESTUDIANTE', 'FUNCIONARIO' o 'MIXTO'.
--     ESTUDIANTE  -> solo alumnos de tipo Estudiante.
--     FUNCIONARIO -> Docente, Administrativo y Contratista.
--     MIXTO       -> toda la comunidad universitaria.
--
-- Datos existentes: todos los cursos quedan 'MIXTO' (el DEFAULT rellena las filas existentes), asi que nadie
-- pierde acceso a nada. El Coordinador cambia el publico desde el formulario del curso.
--
-- Quien lo ejecuta: una persona, manualmente, una vez por base de datos (ddl-auto=none). Requiere MySQL 8.0.16+.
-- Ejemplo: mysql --default-character-set=utf8mb4 -h<host> -P<puerto> -u<usuario> -p <basededatos> < db/migraciones/2026-11-03_publico_curso.sql
--
-- IDEMPOTENTE: la columna y el CHECK se verifican en INFORMATION_SCHEMA antes de crearse; correrlo dos veces no
-- falla ni cambia nada.
-- ============================================================

-- 1) tbl_curso.CUR_PUBLICO
SET @columna_existe = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_curso'
      AND COLUMN_NAME = 'CUR_PUBLICO'
);
SET @sql_alter = IF(
    @columna_existe = 0,
    'ALTER TABLE tbl_curso ADD COLUMN CUR_PUBLICO VARCHAR(15) NOT NULL DEFAULT ''MIXTO''',
    'SELECT 1'
);
PREPARE stmt_alter FROM @sql_alter;
EXECUTE stmt_alter;
DEALLOCATE PREPARE stmt_alter;

-- 2) CKC_CURSO_PUBLICO
SET @check_existe = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'CKC_CURSO_PUBLICO'
);
SET @sql_check = IF(
    @check_existe = 0,
    'ALTER TABLE tbl_curso ADD CONSTRAINT CKC_CURSO_PUBLICO CHECK (CUR_PUBLICO IN (''ESTUDIANTE'', ''FUNCIONARIO'', ''MIXTO''))',
    'SELECT 1'
);
PREPARE stmt_check FROM @sql_check;
EXECUTE stmt_check;
DEALLOCATE PREPARE stmt_check;
