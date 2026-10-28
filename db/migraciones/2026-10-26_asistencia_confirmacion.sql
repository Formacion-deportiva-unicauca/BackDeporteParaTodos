-- ============================================================
-- Migracion: 2026-10-26_asistencia_confirmacion.sql
-- Faltas acumuladas por alumno (etapa 2 de 5).
--
-- Que crea:
--   - tbl_clase.CLS_ASISTENCIA_CONFIRMADA_EN  DATETIME     NULL
--   - tbl_clase.CLS_ASISTENCIA_CONFIRMADA_POR VARCHAR(50)  NULL
--   - tbl_asistencia.ASI_ESTADO               VARCHAR(10)  NOT NULL DEFAULT 'PRESENTE'
--   - Indice IDX_ASISTENCIA_CLASE_ESTADO_PERFIL (CLS_CODIGO, ASI_ESTADO, PERF_ID)
--     sobre tbl_asistencia (candidato propuesto para la consulta agregada de
--     faltas de la etapa 5; no validado con EXPLAIN contra un MySQL real).
-- Tipo, nombre y nulabilidad IDENTICOS a src/main/resources/DDL-MYSQL.sql --
-- cualquier cambio futuro debe aplicarse en AMBOS archivos a la vez.
--
-- Por que NO hay CHECK en ASI_ESTADO: CHECK solo se aplica realmente desde
-- MySQL 8.0.16 (antes se acepta en el parser pero se ignora en silencio). No
-- se confirmo la version del servidor de pruebas al escribir esta migracion
-- -- la validacion de PRESENTE/AUSENTE queda en el servicio (etapa 4).
--
-- Por que el DEFAULT 'PRESENTE' es seguro para filas existentes: antes de
-- esta columna, toda fila de tbl_asistencia (eliminada logicamente o no)
-- representaba una presencia -- el modelo viejo nunca guardaba una ausencia
-- como fila. ADD COLUMN con DEFAULT solo rellena la columna nueva; no toca
-- META_ELIMINADO en ninguna fila, eliminada o no. No hay (ni debe haber) un
-- UPDATE adicional despues del ALTER: un UPDATE masivo en una segunda
-- corrida, en un ambiente donde la app ya genero filas AUSENTE reales entre
-- una corrida y la siguiente, las sobreescribiria por error a PRESENTE.
--
-- Contra que base se aplica: una base MySQL 8.0+ EXISTENTE de
-- BackDeporteParaTodos (ya tiene tbl_clase y tbl_asistencia creadas) a la
-- que no se le puede aplicar DDL-MYSQL.sql completo porque borraria datos
-- (ese archivo empieza con DROP TABLE). NO se usa en la CI ni en una base
-- nueva -- una base nueva ya recibe estas columnas e indice al cargar
-- DDL-MYSQL.sql completo.
--
-- Quien lo ejecuta: una persona, manualmente, una sola vez por base de
-- datos (ddl-auto=none en este proyecto -- no hay migraciones automaticas).
-- Ejemplo: mysql -h<host> -P<puerto> -u<usuario> -p <basededatos> < db/migraciones/2026-10-26_asistencia_confirmacion.sql
--
-- IDEMPOTENTE: igual que 2026-10-15_clase_justificacion.sql -- MySQL 8.0 no
-- soporta "ADD COLUMN IF NOT EXISTS" ni "CREATE INDEX IF NOT EXISTS", asi
-- que cada cambio se verifica contra INFORMATION_SCHEMA antes de construirse
-- dinamicamente con PREPARE/EXECUTE y se omite si ya existe. Correr este
-- script dos veces no falla, no duplica nada y no reescribe ASI_ESTADO de
-- filas ya existentes (el ALTER completo se omite por entero en la segunda
-- corrida).
-- ============================================================

-- 1) tbl_clase.CLS_ASISTENCIA_CONFIRMADA_EN
SET @columna_existe = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_clase'
      AND COLUMN_NAME = 'CLS_ASISTENCIA_CONFIRMADA_EN'
);
SET @sql_alter = IF(
    @columna_existe = 0,
    'ALTER TABLE tbl_clase ADD COLUMN CLS_ASISTENCIA_CONFIRMADA_EN DATETIME NULL',
    'SELECT 1'
);
PREPARE stmt_alter FROM @sql_alter;
EXECUTE stmt_alter;
DEALLOCATE PREPARE stmt_alter;

-- 2) tbl_clase.CLS_ASISTENCIA_CONFIRMADA_POR
SET @columna_existe = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_clase'
      AND COLUMN_NAME = 'CLS_ASISTENCIA_CONFIRMADA_POR'
);
SET @sql_alter = IF(
    @columna_existe = 0,
    'ALTER TABLE tbl_clase ADD COLUMN CLS_ASISTENCIA_CONFIRMADA_POR VARCHAR(50) NULL',
    'SELECT 1'
);
PREPARE stmt_alter FROM @sql_alter;
EXECUTE stmt_alter;
DEALLOCATE PREPARE stmt_alter;

-- 3) tbl_asistencia.ASI_ESTADO
SET @columna_existe = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_asistencia'
      AND COLUMN_NAME = 'ASI_ESTADO'
);
SET @sql_alter = IF(
    @columna_existe = 0,
    'ALTER TABLE tbl_asistencia ADD COLUMN ASI_ESTADO VARCHAR(10) NOT NULL DEFAULT ''PRESENTE''',
    'SELECT 1'
);
PREPARE stmt_alter FROM @sql_alter;
EXECUTE stmt_alter;
DEALLOCATE PREPARE stmt_alter;

-- 4) Indice IDX_ASISTENCIA_CLASE_ESTADO_PERFIL sobre tbl_asistencia
SET @indice_existe = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_asistencia'
      AND INDEX_NAME = 'IDX_ASISTENCIA_CLASE_ESTADO_PERFIL'
);
SET @sql_indice = IF(
    @indice_existe = 0,
    'CREATE INDEX IDX_ASISTENCIA_CLASE_ESTADO_PERFIL ON tbl_asistencia (CLS_CODIGO, ASI_ESTADO, PERF_ID)',
    'SELECT 1'
);
PREPARE stmt_indice FROM @sql_indice;
EXECUTE stmt_indice;
DEALLOCATE PREPARE stmt_indice;
