-- ============================================================
-- Migracion: 2026-10-12_plazo_carga_atenciones.sql
-- SCRUM-80 (HE-14 HU-02): crea tbl_plazo_carga_atenciones.
--
-- Que crea: la tabla tbl_plazo_carga_atenciones (plazo de carga de
-- atenciones que el Coordinador asigna a un instructor por periodo
-- academico), con su FK hacia tbl_instructor. Columnas, tipos, claves e
-- indices IDENTICOS al bloque correspondiente de
-- src/main/resources/DDL-MYSQL.sql -- cualquier cambio futuro debe aplicarse
-- en AMBOS archivos a la vez.
--
-- Contra que base se aplica: una base MySQL 8.0+ EXISTENTE de
-- BackDeporteParaTodos (ya tiene tbl_instructor creada) a la que no se le
-- puede aplicar DDL-MYSQL.sql completo porque borraria datos (ese archivo
-- empieza con DROP TABLE). NO se usa en la CI ni en una base nueva -- una
-- base nueva ya recibe esta tabla al cargar DDL-MYSQL.sql completo.
--
-- Quien lo ejecuta: una persona, manualmente, una sola vez por base de
-- datos (ddl-auto=none en este proyecto -- no hay migraciones automaticas).
-- Ejemplo: mysql -h<host> -P<puerto> -u<usuario> -p <basededatos> < db/migraciones/2026-10-12_plazo_carga_atenciones.sql
--
-- IDEMPOTENTE: CREATE TABLE IF NOT EXISTS, sin DROP ni DELETE -- correrlo
-- dos veces no borra ni duplica nada; si la tabla ya existe, no hace nada.
-- ============================================================

CREATE TABLE IF NOT EXISTS tbl_plazo_carga_atenciones (
    META_ELIMINADO       INT          NOT NULL DEFAULT 0,
    PERF_ID              VARCHAR(50)  NOT NULL,
    PLZ_ANIO             BIGINT       NOT NULL,
    PLZ_PERIODO          TINYINT      NOT NULL,
    PLZ_FECHA_LIMITE     DATE         NOT NULL,
    PLZ_COORDINADOR      VARCHAR(50)  NOT NULL,
    PLZ_FECHA_ASIGNACION DATETIME     NOT NULL,
    CONSTRAINT FK_PLAZO_CARGA_INSTRUCTOR      FOREIGN KEY (PERF_ID) REFERENCES tbl_instructor (PERF_ID) ON DELETE RESTRICT,
    CONSTRAINT PK_TBL_PLAZO_CARGA_ATENCIONES  PRIMARY KEY (PERF_ID, PLZ_ANIO, PLZ_PERIODO)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
