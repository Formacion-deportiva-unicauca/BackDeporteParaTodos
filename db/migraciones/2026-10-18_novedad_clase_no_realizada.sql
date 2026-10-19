-- ============================================================
-- Migracion: 2026-10-18_novedad_clase_no_realizada.sql
-- Registro de novedades de clases no realizadas: crea tbl_novedad_clase.
--
-- Que crea: la tabla tbl_novedad_clase (novedad de clase no realizada),
-- con su FK hacia tbl_grupo y una restricción UNIQUE para garantizar
-- s�lo una novedad activa por grupo y fecha.
--
-- Contra qu� base se aplica: una base MySQL 8.0+ EXISTENTE de
-- BackDeporteParaTodos (ya tiene tbl_grupo creada) a la que no se le
-- puede aplicar DDL-MYSQL.sql completo porque borrar�a datos.
-- NO se usa en la CI ni en una base nueva -- una base nueva ya recibe
-- esta tabla al cargar DDL-MYSQL.sql completo.
--
-- Qui�n lo ejecuta: una persona, manualmente, una sola vez por base de
-- datos (ddl-auto=none en este proyecto -- no hay migraciones autom�ticas).
-- Ejemplo: mysql -h<host> -P<puerto> -u<usuario> -p <basededatos> < db/migraciones/2026-10-18_novedad_clase_no_realizada.sql
--
-- IDEMPOTENTE: CREATE TABLE IF NOT EXISTS, sin DROP ni DELETE -- correrlo
-- dos veces no borra ni duplica nada; si la tabla ya existe, no hace nada.
-- ============================================================

CREATE TABLE IF NOT EXISTS tbl_novedad_clase (
    NOV_CODIGO            BIGINT       NOT NULL AUTO_INCREMENT,
    CAT_TITULO            VARCHAR(100) NOT NULL,
    CUR_NOMBRE            VARCHAR(100) NOT NULL,
    GRP_ANIO              BIGINT       NOT NULL,
    GRP_ITERABLE          BIGINT       NOT NULL,
    NOV_FECHA             DATE         NOT NULL,
    NOV_MOTIVO            VARCHAR(50)  NOT NULL,
    NOV_DESCRIPCION       VARCHAR(1000),
    NOV_USUARIO_CREADOR   VARCHAR(50)  NOT NULL,
    NOV_FECHA_CREACION    DATETIME     NOT NULL,
    NOV_USUARIO_MODIFICADOR VARCHAR(50),
    NOV_FECHA_MODIFICACION DATETIME,
    NOV_VERSION           INT          NOT NULL DEFAULT 0,
    META_ELIMINADO        INT          NOT NULL DEFAULT 0,
    CONSTRAINT FK_NOVEDAD_GRUPO FOREIGN KEY (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE)
        REFERENCES tbl_grupo (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE) ON DELETE RESTRICT,
    CONSTRAINT PK_TBL_NOVEDAD_CLASE PRIMARY KEY (NOV_CODIGO),
    CONSTRAINT UQ_TBL_NOVEDAD_CLASE UNIQUE (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE, NOV_FECHA, META_ELIMINADO)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
