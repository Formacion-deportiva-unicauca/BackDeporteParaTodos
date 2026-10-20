-- ============================================================
-- Migracion: 2026-10-15_clase_justificacion.sql
-- SCRUM-80 (rediseno, bloque 2 de 3): agrega CLS_JUSTIFICACION a tbl_clase.
--
-- Que crea: la columna CLS_JUSTIFICACION VARCHAR(500) NULL en tbl_clase --
-- guarda la justificacion obligatoria cuando el grupo no tiene franja de
-- horario el dia de la fecha de la clase (ClaseServicio la exige solo en ese
-- caso; la columna en si es nullable, igual que CLS_OBSERVACION). Tipo,
-- nombre y nulabilidad IDENTICOS al bloque correspondiente de
-- src/main/resources/DDL-MYSQL.sql -- cualquier cambio futuro debe aplicarse
-- en AMBOS archivos a la vez.
--
-- Contra que base se aplica: una base MySQL 8.0+ EXISTENTE de
-- BackDeporteParaTodos (ya tiene tbl_clase creada) a la que no se le puede
-- aplicar DDL-MYSQL.sql completo porque borraria datos (ese archivo empieza
-- con DROP TABLE). NO se usa en la CI ni en una base nueva -- una base nueva
-- ya recibe esta columna al cargar DDL-MYSQL.sql completo.
--
-- Quien lo ejecuta: una persona, manualmente, una sola vez por base de
-- datos (ddl-auto=none en este proyecto -- no hay migraciones automaticas).
-- Ejemplo: mysql -h<host> -P<puerto> -u<usuario> -p <basededatos> < db/migraciones/2026-10-15_clase_justificacion.sql
--
-- IDEMPOTENTE: MySQL 8.0 NO soporta "ALTER TABLE ... ADD COLUMN IF NOT
-- EXISTS" (es sintaxis de MariaDB, no de MySQL) -- verificado contra
-- INFORMATION_SCHEMA.COLUMNS antes de intentar el ALTER, y el ALTER real se
-- arma dinamicamente con PREPARE/EXECUTE para poder omitirlo del todo si la
-- columna ya existe. Correr este script dos veces no falla ni duplica nada.
-- ============================================================

SET @columna_existe = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_clase'
      AND COLUMN_NAME = 'CLS_JUSTIFICACION'
);

SET @sql_alter = IF(
    @columna_existe = 0,
    'ALTER TABLE tbl_clase ADD COLUMN CLS_JUSTIFICACION VARCHAR(500) NULL',
    'SELECT 1'
);

PREPARE stmt_alter FROM @sql_alter;
EXECUTE stmt_alter;
DEALLOCATE PREPARE stmt_alter;
