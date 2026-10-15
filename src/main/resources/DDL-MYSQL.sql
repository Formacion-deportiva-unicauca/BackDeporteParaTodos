-- ============================================================
-- DDL-MYSQL.sql — Esquema BackDeporteParaTodos para MySQL 8.0+
-- Migrado desde Oracle (DROP-CREATE.sql + AQLActual.sql)
-- ============================================================
-- Conversiones aplicadas:
--   VARCHAR2(N BYTE) -> VARCHAR(N)
--   NUMBER(*,0) PK con secuencia -> BIGINT AUTO_INCREMENT
--   NUMBER(*,0) general -> BIGINT
--   NUMBER(1) -> TINYINT
--   NUMBER(3) -> SMALLINT
--   BLOB -> LONGBLOB
--   Oracle DATE + java.sql.Date/LocalDate -> DATE
--   Oracle DATE + java.sql.Timestamp -> DATETIME
--   Secuencias eliminadas (SEQ_CLS_CODIGO, SEQ_ID_HORARIO,
--                          SEQ_ID_IMAGEN, SEQ_ESC_ID)
--   ALTER TABLE CUR_ESTADO_INSCRIPCIONES y GRP_PERIODO integrados
--   Directivas SQL*Plus eliminadas (REM, SET DEFINE OFF, COMMENT ON)
--   NOTIFICACION_ENTRADA omitida (sin CREATE TABLE en el DDL de origen)
-- ------------------------------------------------------------
-- SCRUM-166: correccion de 15 conflictos NOT NULL + ON DELETE SET NULL
--   (Error 1830 en MySQL; Oracle no valida esto en DDL time)
--   Patron aplicado por caso:
--     CASCADE (6):  #1 INSTRUCTOR, #6 ALUMNO, #7/#8 INTERMEDIA,
--                   #11 HORARIO, #15 ALERTA
--     RESTRICT (8): #2 CURSO, #3 GRUPO, #5 CLASE-grupo,
--                   #9/#10 ASISTENCIA, #12/#13 INSCRIPCION, #14 NOTIFICACION
--     Quitar NOT NULL + SET NULL (1): #4 CLASE.PERF_ID (instructor opcional)
-- ------------------------------------------------------------
-- HALLAZGOS ADICIONALES SCRUM-166 (resueltos):
--   1) tbl_coordinador.PERF_ID no tenia FK hacia tbl_perfil (inconsistencia
--      preexistente del DDL de Oracle). Resuelto: FK_COORDINADOR_PERFIL con
--      ON DELETE CASCADE, igual que INSTRUCTOR (#1) y ALUMNO (#6).
--   2) Registro huerfano tbl_instructor.PERF_ID='987445' (sin perfil padre):
--      nunca existio en este esquema -- el INSERT original se omitio en la
--      migracion, y se verifico que no esta en la BD MySQL ni en el backup
--      previo a la migracion. Sin accion pendiente.
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------
-- DROP (orden inverso a dependencias, FOREIGN_KEY_CHECKS=0)
-- --------------------------------------------------------
DROP TABLE IF EXISTS tbl_alerta;
DROP TABLE IF EXISTS tbl_notificacion;
DROP TABLE IF EXISTS tbl_bandeja_entrada;
DROP TABLE IF EXISTS tbl_bandeja_salida;
DROP TABLE IF EXISTS tbl_asistencia;
DROP TABLE IF EXISTS tbl_inscripcion;
DROP TABLE IF EXISTS tbl_horario;
DROP TABLE IF EXISTS tbl_clase;
DROP TABLE IF EXISTS tbl_intermedia_alumno_programa;
DROP TABLE IF EXISTS tbl_alumno;
DROP TABLE IF EXISTS tbl_grupo;
DROP TABLE IF EXISTS tbl_curso;
DROP TABLE IF EXISTS tbl_coordinador;
DROP TABLE IF EXISTS tbl_plazo_carga_atenciones;
DROP TABLE IF EXISTS tbl_prorroga_registro_asistencia;
DROP TABLE IF EXISTS tbl_instructor;
DROP TABLE IF EXISTS tbl_programa;
DROP TABLE IF EXISTS tbl_facultad;
DROP TABLE IF EXISTS tbl_categoria_curso;
DROP TABLE IF EXISTS tbl_perfil;
DROP TABLE IF EXISTS tbl_imagen;
DROP TABLE IF EXISTS tbl_deporte;
DROP TABLE IF EXISTS tbl_escenario;

SET FOREIGN_KEY_CHECKS = 1;

-- --------------------------------------------------------
-- tbl_deporte
-- --------------------------------------------------------
CREATE TABLE tbl_deporte (
    META_ELIMINADO INT          NOT NULL,
    DEPT_NOMBRE    VARCHAR(100) NOT NULL,
    CONSTRAINT PK_TBL_DEPORTE PRIMARY KEY (DEPT_NOMBRE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_imagen
-- IMG_ID: era SEQ_ID_IMAGEN (START 201) -> AUTO_INCREMENT
-- IMG_DATOS: BLOB -> LONGBLOB
-- --------------------------------------------------------
CREATE TABLE tbl_imagen (
    META_ELIMINADO   INT          NOT NULL,
    IMG_ID           BIGINT       NOT NULL AUTO_INCREMENT,
    IMG_NOMBRE       VARCHAR(200) NOT NULL,
    IMG_TIPO_ARCHIVO VARCHAR(100) NOT NULL,
    IMG_LONGITUD     BIGINT       NOT NULL,
    IMG_DATOS        LONGBLOB     NOT NULL,
    CONSTRAINT PK_TBL_IMAGEN PRIMARY KEY (IMG_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_categoria_curso
-- FK_CATEGORIA_CURSO_IMAGEN: imagen es opcional -> SET NULL OK (columna nullable)
-- --------------------------------------------------------
CREATE TABLE tbl_categoria_curso (
    META_ELIMINADO  INT           NOT NULL,
    CAT_TITULO      VARCHAR(100)  NOT NULL,
    CAT_DESCRIPCION VARCHAR(1000) NOT NULL,
    CAT_IMAGEN      BIGINT,
    CONSTRAINT PK_TBL_CATEGORIA_CURSO    PRIMARY KEY (CAT_TITULO),
    CONSTRAINT FK_CATEGORIA_CURSO_IMAGEN FOREIGN KEY (CAT_IMAGEN) REFERENCES tbl_imagen (IMG_ID) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_perfil
-- FK_PERFIL_IMAGEN: sin ON DELETE -> RESTRICT por defecto (imagen no deberia borrarse)
-- --------------------------------------------------------
CREATE TABLE tbl_perfil (
    META_ELIMINADO INT          NOT NULL,
    PERF_ID        VARCHAR(50)  NOT NULL,
    PERF_NOMBRE    VARCHAR(100) NOT NULL,
    PERF_CORREO    VARCHAR(100) NOT NULL,
    PERF_IMAGEN    BIGINT,
    PERF_TIPOID    VARCHAR(50)  NOT NULL,
    PERF_SEXO      VARCHAR(10)  NOT NULL,
    CONSTRAINT CKC_TBL_PERF_SEXO   CHECK (PERF_SEXO   IN ('M', 'F')),
    CONSTRAINT CKC_TBL_PERF_TIPOID CHECK (PERF_TIPOID IN ('CC', 'TI', 'CE', 'PP', 'PEP', 'DIE')),
    CONSTRAINT FK_PERFIL_IMAGEN     FOREIGN KEY (PERF_IMAGEN) REFERENCES tbl_imagen (IMG_ID),
    CONSTRAINT PK_TBL_PERFIL        PRIMARY KEY (PERF_ID),
    CONSTRAINT UQ_PERFIL_CORREO     UNIQUE (PERF_CORREO)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_instructor
-- SCRUM-166 #1: PERF_ID es PK -> SET NULL imposible -> CASCADE
--   Un instructor es una especializacion de perfil; si el perfil
--   se borra fisicamente, el registro de instructor no tiene existencia propia.
-- --------------------------------------------------------
CREATE TABLE tbl_instructor (
    META_ELIMINADO INT         NOT NULL,
    PERF_ID        VARCHAR(50) NOT NULL,
    CONSTRAINT FK_INSTRUCTOR_PERFIL FOREIGN KEY (PERF_ID) REFERENCES tbl_perfil (PERF_ID) ON DELETE CASCADE,
    CONSTRAINT PF_TBL_INSTRUCTOR    PRIMARY KEY (PERF_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_plazo_carga_atenciones
-- SCRUM-80 (HE-14 HU-02): plazo de carga de atenciones que el Coordinador
-- asigna a un instructor para un periodo (PLZ_ANIO, PLZ_PERIODO) especifico
-- del periodo academico (misma regla de periodo que GRP_PERIODO: 1 o 2).
-- FK_PLAZO_CARGA_INSTRUCTOR: mismo criterio que FK_GRUPO_INSTRUCTOR/
-- FK_CLASE_INSTRUCTOR -- RESTRICT (el instructor no se borra fisicamente).
-- PLZ_COORDINADOR guarda solo el identificador del coordinador que asigno/
-- reemplazo el plazo (rastro de auditoria); sin FK, igual que otras columnas
-- de solo-identificador en el esquema (p.ej. no hay FK de auditoria en
-- tbl_asistencia hacia quien la registro).
-- --------------------------------------------------------
CREATE TABLE tbl_plazo_carga_atenciones (
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

-- --------------------------------------------------------
-- tbl_prorroga_registro_asistencia
-- SCRUM-80 (rediseno, 15 oct 2026, bloque 1 de 3): reemplaza el enfoque de
-- tbl_plazo_carga_atenciones (que queda tal cual, sin usarse en el diseno
-- nuevo, hasta que el frontend migre). El Coordinador asigna una prorroga en
-- DIAS por instructor y por mes CALENDARIO (PRR_ANIO, PRR_MES); el limite
-- (PRR_FECHA_LIMITE) se PRECALCULA al asignar: max(fin de mes, fecha de
-- asignacion) + dias (ver VentanaRegistroAsistencia.calcularLimiteProrroga).
-- dias=0 borra la fila FISICAMENTE (es configuracion, no historial) -- por
-- eso no tiene su propio borrado logico aparte de META_ELIMINADO (se deja
-- por convencion con las demas tablas, pero esta tabla nunca guarda una fila
-- con META_ELIMINADO=1: la fila simplemente se elimina).
-- FK_PRORROGA_INSTRUCTOR: mismo criterio que FK_PLAZO_CARGA_INSTRUCTOR --
-- RESTRICT (el instructor no se borra fisicamente).
-- --------------------------------------------------------
CREATE TABLE tbl_prorroga_registro_asistencia (
    META_ELIMINADO       INT          NOT NULL DEFAULT 0,
    PERF_ID              VARCHAR(50)  NOT NULL,
    PRR_ANIO             BIGINT       NOT NULL,
    PRR_MES              TINYINT      NOT NULL,
    PRR_DIAS             INT          NOT NULL,
    PRR_FECHA_LIMITE     DATE         NOT NULL,
    PRR_COORDINADOR      VARCHAR(50)  NOT NULL,
    PRR_FECHA_ASIGNACION DATETIME     NOT NULL,
    CONSTRAINT FK_PRORROGA_INSTRUCTOR      FOREIGN KEY (PERF_ID) REFERENCES tbl_instructor (PERF_ID) ON DELETE RESTRICT,
    CONSTRAINT PK_TBL_PRORROGA_REGISTRO_ASISTENCIA PRIMARY KEY (PERF_ID, PRR_ANIO, PRR_MES)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_facultad
-- --------------------------------------------------------
CREATE TABLE tbl_facultad (
    META_ELIMINADO INT          NOT NULL,
    FAC_NOMBRE     VARCHAR(200),
    CONSTRAINT PK_TBL_FACULTAD PRIMARY KEY (FAC_NOMBRE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_programa
-- FK_PROGRAMA_FACULTAD: FAC_NOMBRE es nullable -> SET NULL OK
-- --------------------------------------------------------
CREATE TABLE tbl_programa (
    META_ELIMINADO INT          NOT NULL,
    PRG_NOMBRE     VARCHAR(200) NOT NULL,
    FAC_NOMBRE     VARCHAR(200),
    CONSTRAINT PK_TBL_PROGRAMA      PRIMARY KEY (PRG_NOMBRE),
    CONSTRAINT FK_PROGRAMA_FACULTAD FOREIGN KEY (FAC_NOMBRE) REFERENCES tbl_facultad (FAC_NOMBRE) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_curso
-- SCRUM-166 #2: CAT_TITULO NOT NULL y parte de PK -> SET NULL imposible -> RESTRICT
--   La categoria es co-PK del curso; no puede quedar sin categoria.
--   El sistema usa soft-delete: el borrado fisico de categorias no deberia ocurrir.
-- FK_TBL_CURSO_DEPORTE: DEPT_NOMBRE nullable -> SET NULL OK
-- FK_TBL_CURSO_IMAGEN:  CUR_IMAGEN nullable -> SET NULL OK
-- CUR_ESTADO_INSCRIPCIONES: integrado desde ALTER TABLE del DDL original
-- --------------------------------------------------------
CREATE TABLE tbl_curso (
    META_ELIMINADO           INT           NOT NULL,
    CUR_NOMBRE               VARCHAR(100)  NOT NULL,
    DEPT_NOMBRE              VARCHAR(100),
    CAT_TITULO               VARCHAR(100)  NOT NULL,
    CUR_DESCRIPCION          VARCHAR(1000) NOT NULL,
    CUR_IMAGEN               BIGINT,
    CUR_ESTADO_INSCRIPCIONES VARCHAR(10)   DEFAULT 'ABIERTO',
    CONSTRAINT FK_TBL_CURSO_DEPORTE   FOREIGN KEY (DEPT_NOMBRE) REFERENCES tbl_deporte (DEPT_NOMBRE)        ON DELETE SET NULL,
    CONSTRAINT FK_TBL_CURSO_IMAGEN    FOREIGN KEY (CUR_IMAGEN)  REFERENCES tbl_imagen (IMG_ID)              ON DELETE SET NULL,
    CONSTRAINT FK_TBL_CURSO_CATEGORIA FOREIGN KEY (CAT_TITULO)  REFERENCES tbl_categoria_curso (CAT_TITULO) ON DELETE RESTRICT,
    CONSTRAINT PK_TBL_CURSO           PRIMARY KEY (CUR_NOMBRE, CAT_TITULO)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_grupo
-- SCRUM-166 #3: CUR_NOMBRE, CAT_TITULO son parte de PK (implicitement NOT NULL)
--   -> SET NULL imposible -> RESTRICT
--   El grupo no existe sin curso. El curso no se borra fisicamente.
-- FK_GRUPO_IMAGEN:     GRP_IMAGEN nullable -> SET NULL OK
-- FK_GRUPO_INSTRUCTOR: sin ON DELETE -> RESTRICT por defecto (instructor puede cambiar)
-- GRP_PERIODO: integrado desde ALTER TABLE del DDL original; NUMBER(1) -> TINYINT
-- GRP_FECHACREACION etc: Java usa LocalDate -> MySQL DATE
-- --------------------------------------------------------
CREATE TABLE tbl_grupo (
    META_ELIMINADO             BIGINT,
    GRP_ANIO                   BIGINT,
    GRP_ITERABLE               BIGINT,
    CUR_NOMBRE                 VARCHAR(100),
    CAT_TITULO                 VARCHAR(100),
    GRP_IMAGEN                 BIGINT,
    GRP_CUPOS                  BIGINT,
    GRP_FECHACREACION          DATE,
    GRP_FECHA_FINALIZACION     DATE,
    GRP_FECHA_INSCRIP_APERTURA DATE,
    GRP_FECHA_INSCRIP_CIERRE   DATE,
    PERF_ID                    VARCHAR(50),
    GRP_PERIODO                TINYINT      DEFAULT 1 NOT NULL,
    CONSTRAINT FK_GRUPO_IMAGEN     FOREIGN KEY (GRP_IMAGEN)             REFERENCES tbl_imagen (IMG_ID)                      ON DELETE SET NULL,
    CONSTRAINT FK_GRUPO_CURSO      FOREIGN KEY (CUR_NOMBRE, CAT_TITULO) REFERENCES tbl_curso (CUR_NOMBRE, CAT_TITULO)       ON DELETE RESTRICT,
    CONSTRAINT FK_GRUPO_INSTRUCTOR FOREIGN KEY (PERF_ID)                REFERENCES tbl_instructor (PERF_ID),
    CONSTRAINT PK_TBL_GRUPO        PRIMARY KEY (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_clase
-- CLS_CODIGO: era SEQ_CLS_CODIGO (START 121) -> AUTO_INCREMENT
-- CLS_FECHA: Java usa java.sql.Date -> MySQL DATE
--   (la hora se gestiona en tbl_horario; comentario Oracle obsoleto)
-- SCRUM-166 #4: PERF_ID -> se quita NOT NULL + se mantiene SET NULL
--   Una clase puede quedar sin instructor asignado (instructor que se da de baja).
--   NULL es semanticamente valido y la entidad Java no tiene nullable=false.
-- SCRUM-166 #5: FK_CLASE_GRUPO con columnas NOT NULL -> RESTRICT
--   Una clase sin grupo no tiene sentido; el grupo no se borra fisicamente.
-- --------------------------------------------------------
CREATE TABLE tbl_clase (
    META_ELIMINADO       INT          NOT NULL,
    CLS_CODIGO           BIGINT       NOT NULL AUTO_INCREMENT,
    PERF_ID              VARCHAR(50),
    CLS_FECHA            DATE         NOT NULL,
    CLS_DURACION_HORAS   INT          NOT NULL,
    CLS_DURACION_MINUTOS INT          NOT NULL,
    CLS_OBSERVACION      VARCHAR(1000),
    GRP_ANIO             BIGINT       NOT NULL,
    GRP_ITERABLE         BIGINT       NOT NULL,
    CUR_NOMBRE           VARCHAR(100) NOT NULL,
    CAT_TITULO           VARCHAR(100) NOT NULL,
    CONSTRAINT FK_CLASE_INSTRUCTOR FOREIGN KEY (PERF_ID)                                        REFERENCES tbl_instructor (PERF_ID)                                    ON DELETE SET NULL,
    CONSTRAINT FK_CLASE_GRUPO      FOREIGN KEY (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE) REFERENCES tbl_grupo (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE) ON DELETE RESTRICT,
    CONSTRAINT PK_TBL_CLASE        PRIMARY KEY (CLS_CODIGO)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_alumno
-- SCRUM-166 #6: PERF_ID es PK -> SET NULL imposible -> CASCADE
--   Un alumno es una especializacion de perfil; si el perfil
--   se borra fisicamente, el registro de alumno no tiene existencia propia.
-- --------------------------------------------------------
CREATE TABLE tbl_alumno (
    META_ELIMINADO INT         NOT NULL,
    PERF_ID        VARCHAR(50) NOT NULL,
    ALM_CODIGO     VARCHAR(20),
    ALM_TIPO       VARCHAR(20) NOT NULL,
    ALM_ESTADO     INT,
    CONSTRAINT CKC_ALM_TIPO_ALUM CHECK (ALM_TIPO IN ('Estudiante', 'Administrativo', 'Docente')),
    CONSTRAINT FK_ALUMNO_PERFIL  FOREIGN KEY (PERF_ID) REFERENCES tbl_perfil (PERF_ID) ON DELETE CASCADE,
    CONSTRAINT PERF_ID           PRIMARY KEY (PERF_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_intermedia_alumno_programa
-- SCRUM-166 #7: PRG_NOMBRE parte de PK -> SET NULL imposible -> CASCADE
--   Si el programa desaparece, la relacion alumno-programa pierde significado.
-- SCRUM-166 #8: PERF_ID parte de PK -> SET NULL imposible -> CASCADE
--   Si el alumno desaparece, sus relaciones con programas tambien.
-- --------------------------------------------------------
CREATE TABLE tbl_intermedia_alumno_programa (
    META_ELIMINADO INT          NOT NULL,
    PERF_ID        VARCHAR(50)  NOT NULL,
    PRG_NOMBRE     VARCHAR(200) NOT NULL,
    CONSTRAINT PK_INTERMEDIA_ALUMNO_PROGRAMA PRIMARY KEY (PERF_ID, PRG_NOMBRE),
    CONSTRAINT FK_INTERMEDIA_PROGRAMA FOREIGN KEY (PRG_NOMBRE) REFERENCES tbl_programa (PRG_NOMBRE) ON DELETE CASCADE,
    CONSTRAINT FK_INTERMEDIA_ALUMNO   FOREIGN KEY (PERF_ID)    REFERENCES tbl_alumno (PERF_ID)      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_asistencia
-- SCRUM-166 #9:  CLS_CODIGO parte de PK -> SET NULL imposible -> RESTRICT
--   La asistencia es registro historico; no se debe borrar una clase
--   que ya tiene asistencias registradas.
-- SCRUM-166 #10: PERF_ID parte de PK -> SET NULL imposible -> RESTRICT
--   El historial de asistencia de un alumno debe preservarse como evidencia.
-- --------------------------------------------------------
CREATE TABLE tbl_asistencia (
    META_ELIMINADO INT         NOT NULL,
    PERF_ID        VARCHAR(50) NOT NULL,
    CLS_CODIGO     BIGINT      NOT NULL,
    CONSTRAINT FK_ASISTENCIA_CLASE  FOREIGN KEY (CLS_CODIGO) REFERENCES tbl_clase (CLS_CODIGO)  ON DELETE RESTRICT,
    CONSTRAINT FK_ASISTENCIA_ALUMNO FOREIGN KEY (PERF_ID)    REFERENCES tbl_alumno (PERF_ID)    ON DELETE RESTRICT,
    CONSTRAINT PK_TBL_ASISTENCIA    PRIMARY KEY (PERF_ID, CLS_CODIGO)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_horario
-- HR_ID: era SEQ_ID_HORARIO (START 1) -> AUTO_INCREMENT
-- HR_ESCENARIO: almacena nombre del escenario (VARCHAR), sin FK a tbl_escenario
-- SCRUM-166 #11: columnas de FK son NOT NULL -> SET NULL imposible -> CASCADE
--   Un horario sin grupo no tiene razon de existir.
--   Si el grupo se elimina fisicamente, sus horarios deben eliminarse.
-- --------------------------------------------------------
CREATE TABLE tbl_horario (
    META_ELIMINADO INT          NOT NULL,
    HR_ID          BIGINT       NOT NULL AUTO_INCREMENT,
    CAT_TITULO     VARCHAR(100) NOT NULL,
    CUR_NOMBRE     VARCHAR(100) NOT NULL,
    GRP_ANIO       BIGINT       NOT NULL,
    GRP_ITERABLE   BIGINT       NOT NULL,
    HR_DIA         VARCHAR(50)  NOT NULL,
    HR_HORAINICIO  VARCHAR(8)   NOT NULL,
    HR_HORAFIN     VARCHAR(8)   NOT NULL,
    HR_ESCENARIO   VARCHAR(100) NOT NULL,
    CONSTRAINT PK_TBL_HORARIO   PRIMARY KEY (HR_ID),
    CONSTRAINT FK_HORARIO_GRUPO FOREIGN KEY (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE) REFERENCES tbl_grupo (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_inscripcion
-- INSCR_FECHAINSCRIPCION, INSCR_FECHADESVINCULACION:
--   Java usa java.sql.Timestamp con @Temporal(TIMESTAMP) -> MySQL DATETIME
-- SCRUM-166 #12: PERF_ID parte de PK -> SET NULL imposible -> RESTRICT
--   Las inscripciones son registros academicos formales; no se debe
--   borrar un alumno con inscripciones activas o historicas.
-- SCRUM-166 #13: CAT_TITULO/CUR_NOMBRE/GRP_ANIO/GRP_ITERABLE todos PK -> RESTRICT
--   Un grupo con inscripciones no puede borrarse fisicamente.
-- --------------------------------------------------------
CREATE TABLE tbl_inscripcion (
    META_ELIMINADO            INT          NOT NULL,
    INSCR_FECHAINSCRIPCION    DATETIME     NOT NULL,
    INSCR_FECHADESVINCULACION DATETIME,
    INSCR_ESTADO              VARCHAR(10)  NOT NULL DEFAULT 'INSCRITO',
    GRP_ANIO                  BIGINT       NOT NULL,
    GRP_ITERABLE              BIGINT       NOT NULL,
    CUR_NOMBRE                VARCHAR(100) NOT NULL,
    CAT_TITULO                VARCHAR(100) NOT NULL,
    PERF_ID                   VARCHAR(50)  NOT NULL,
    CONSTRAINT FK_INSCRIPCION_ALUMNO FOREIGN KEY (PERF_ID)                                        REFERENCES tbl_alumno (PERF_ID)                                        ON DELETE RESTRICT,
    CONSTRAINT FK_INSCRIPCION_GRUPO  FOREIGN KEY (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE) REFERENCES tbl_grupo (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE) ON DELETE RESTRICT,
    CONSTRAINT PK_TBL_INSCRIPCION    PRIMARY KEY (PERF_ID, CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_coordinador
-- Hallazgo SCRUM-166 (resuelto): PERF_ID es PK -> CASCADE, igual que
--   INSTRUCTOR (#1) y ALUMNO (#6). Un coordinador es una especializacion de
--   perfil; si el perfil se borra fisicamente, el registro de coordinador
--   no tiene existencia propia. No tiene tablas hijas ni valor historico propio.
-- --------------------------------------------------------
CREATE TABLE tbl_coordinador (
    META_ELIMINADO INT         NOT NULL,
    PERF_ID        VARCHAR(50) NOT NULL,
    CONSTRAINT FK_COORDINADOR_PERFIL FOREIGN KEY (PERF_ID) REFERENCES tbl_perfil (PERF_ID) ON DELETE CASCADE,
    CONSTRAINT PK_TBL_COORDINADOR    PRIMARY KEY (PERF_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_bandeja_entrada
-- --------------------------------------------------------
CREATE TABLE tbl_bandeja_entrada (
    META_ELIMINADO INT         NOT NULL,
    PERF_ID        VARCHAR(50) NOT NULL,
    CONSTRAINT PK_TBL_BANDEJA_ENTRADA PRIMARY KEY (PERF_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_bandeja_salida
-- --------------------------------------------------------
CREATE TABLE tbl_bandeja_salida (
    META_ELIMINADO INT         NOT NULL,
    PERF_ID        VARCHAR(50),
    CONSTRAINT PK_TBL_BANDEJA_SALIDA PRIMARY KEY (PERF_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_notificacion
-- NTF_FECHAENVIO: sin entidad Java; DATETIME conservativo (Oracle DATE almacena hora)
-- NTF_IDENTIFICADOR: sin secuencia en Oracle; BIGINT NOT NULL sin AUTO_INCREMENT
--   (estrategia de poblacion desconocida al no existir entidad Java)
-- SCRUM-166 #14: PERF_ID parte de PK -> SET NULL imposible -> RESTRICT
--   Una notificacion sin emisor pierde trazabilidad de auditoria.
-- --------------------------------------------------------
CREATE TABLE tbl_notificacion (
    META_ELIMINADO    INT           NOT NULL,
    NTF_IDENTIFICADOR BIGINT        NOT NULL,
    PERF_ID           VARCHAR(50)   NOT NULL,
    NTF_EMISOR        VARCHAR(50)   NOT NULL,
    NTF_ASUNTO        VARCHAR(150)  NOT NULL,
    NTF_DESCRIPCION   VARCHAR(1000) NOT NULL,
    NTF_FECHAENVIO    DATETIME      NOT NULL,
    NTF_DESTINATARIOS VARCHAR(1000) NOT NULL,
    CONSTRAINT PK_TBL_NOTIFICACION            PRIMARY KEY (NTF_IDENTIFICADOR, PERF_ID),
    CONSTRAINT FK_NOTIFICACION_BANDEJA_SALIDA FOREIGN KEY (PERF_ID) REFERENCES tbl_bandeja_salida (PERF_ID) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_alerta
-- SCRUM-166 #15: todas las columnas de FK son PK -> SET NULL imposible -> CASCADE
--   Las alertas son mensajes del grupo. Si el grupo desaparece, sus alertas tambien.
-- --------------------------------------------------------
CREATE TABLE tbl_alerta (
    META_ELIMINADO    INT           NOT NULL,
    GRP_ANIO          BIGINT        NOT NULL,
    GRP_ITERABLE      BIGINT        NOT NULL,
    CUR_NOMBRE        VARCHAR(100)  NOT NULL,
    CAT_TITULO        VARCHAR(100)  NOT NULL,
    ALERT_ASUNTO      VARCHAR(100)  NOT NULL,
    ALERT_DESCRIPCION VARCHAR(1000) NOT NULL,
    CONSTRAINT FK_ALERTA_CURSO FOREIGN KEY (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE) REFERENCES tbl_grupo (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE) ON DELETE CASCADE,
    CONSTRAINT PK_TBL_ALERTA   PRIMARY KEY (GRP_ANIO, GRP_ITERABLE, CUR_NOMBRE, CAT_TITULO)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- tbl_escenario
-- ESC_ID: era SEQ_ESC_ID (START 1) -> AUTO_INCREMENT
-- --------------------------------------------------------
CREATE TABLE tbl_escenario (
    META_ELIMINADO   TINYINT      DEFAULT 0 NOT NULL,
    ESC_ID           BIGINT       NOT NULL AUTO_INCREMENT,
    ESC_NOMBRE       VARCHAR(100) NOT NULL,
    ESC_DESCRIPCION  VARCHAR(500),
    ESC_NUM_TRIBUNAS SMALLINT     DEFAULT 0 NOT NULL,
    ESC_DISPONIBLE   TINYINT      DEFAULT 1 NOT NULL,
    CONSTRAINT PK_TBL_ESCENARIO        PRIMARY KEY (ESC_ID),
    CONSTRAINT UQ_TBL_ESCENARIO_NOMBRE UNIQUE (ESC_NOMBRE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- DATOS INICIALES (SEED DATA)
-- ============================================================

INSERT INTO tbl_perfil (META_ELIMINADO, PERF_ID, PERF_NOMBRE, PERF_CORREO, PERF_IMAGEN, PERF_TIPOID, PERF_SEXO) VALUES
    (0, '3',          'SMERT EMJ',                       'edynsonjm@gmail.com',          NULL, 'CC', 'M'),
    (0, '1',          'EDYNSON MUNOZ JIMENEZ',           'edinsonmjim@unicauca.edu.co',  NULL, 'CC', 'M'),
    (0, '2',          'edynson munoz jimenez',           'edynsonmj@gmail.com',          NULL, 'CC', 'M'),
    (0, '1141315391', 'MIGUEL ANGEL DORADO QUINAYAS',    'migueladorado@unicauca.edu.co', NULL, 'CC', 'M'),
    (0, '1002958703', 'Yonathan Fernandez Guengue',      'tochavi@unicauca.edu.co',      NULL, 'CC', 'M'),
    (0, '1061786866', 'Luis Alexis Mendez Rojas',        'lamendez@unicauca.edu.co',     NULL, 'CC', 'M'),
    (0, '1061700114', 'Mitchelt Hernan Chilito Villota', 'mhchilito@unicauca.edu.co',    NULL, 'CC', 'M'),
    (0, '1061721857', 'JOHAN RICARDO MENDEZ CASTRO',     'johanmendez@unicauca.edu.co',  NULL, 'TI', 'M'),
    (0, '1061813673', 'Juan Sebastian Pisso',            'jpisso@unicauca.edu.co',       NULL, 'CC', 'M');

INSERT INTO tbl_alumno (META_ELIMINADO, PERF_ID, ALM_CODIGO, ALM_TIPO, ALM_ESTADO) VALUES
    (0, '3',          NULL, 'Estudiante',     NULL),
    (0, '1',          NULL, 'Administrativo', NULL),
    (0, '2',          NULL, 'Docente',        NULL),
    (0, '1141315391', NULL, 'Estudiante',     NULL),
    (0, '1002958703', NULL, 'Estudiante',     NULL),
    (0, '1061786866', NULL, 'Estudiante',     NULL),
    (0, '1061700114', NULL, 'Estudiante',     NULL),
    (0, '1061721857', NULL, 'Estudiante',     NULL);

INSERT INTO tbl_categoria_curso (META_ELIMINADO, CAT_TITULO, CAT_DESCRIPCION, CAT_IMAGEN) VALUES
    (0, 'seleccionado ', 'Es un espacio para el entrenamiento y participacion en eventos deportivos competitivos de caracter universitario, realizados a nivel local, nacional e internacional, bien sea en la red ASCUN o invitaciones de otras universidades.', NULL),
    (0, 'Recreativo',    'El deporte recreativo es la modalidad definida como aquella practicada por placer y diversion, sin ninguna intencion de competir o superar a un adversario.', NULL),
    (0, 'Semillero',     'Este espacio tiene la facultad de ayudar a desarrollar destrezas fisicas, hacer ejercicios, socializar, divertirse, aprender a jugar formando parte de un grupo o equipo, aprender a jugar limpio y a mejorar su autoestima.', NULL),
    (1, 'prueba',        'Ririr8t', NULL),
    (1, 'prurba2',       'Rur7r',   NULL),
    (1, 'ururt',         'Prueba',  NULL);

INSERT INTO tbl_deporte (META_ELIMINADO, DEPT_NOMBRE) VALUES
    (0, 'Futbol'),
    (0, 'Baloncesto'),
    (0, 'Tenis'),
    (0, 'Natacion'),
    (0, 'Ping pong'),
    (0, 'Voleibol'),
    (0, 'Atletismo');

INSERT INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'fiet');

INSERT INTO tbl_curso (META_ELIMINADO, CUR_NOMBRE, DEPT_NOMBRE, CAT_TITULO, CUR_DESCRIPCION, CUR_IMAGEN) VALUES
    (0, 'futbol 1',             'Futbol',     'seleccionado ', 'Este curso es un programa competitivo que busca desarrollar las habilidades tecnicas, tacticas y fisicas de los jugadores para enfrentar a otras facultades e instituciones a nivel local y nacional.', NULL),
    (0, 'baloncesto',           'Baloncesto', 'seleccionado ', 'Este curso es ideal para hombres y mujeres que buscan competir a nivel superior en baloncesto. Se enfoca en la practica de tecnicas avanzadas y la mejora de la condicion fisica.', NULL),
    (0, 'natacion',              'Natacion',   'Semillero',     'Descr', NULL),
    (0, 'Baloncesto masculino', 'Baloncesto', 'Recreativo',    'El baloncesto recreativo es una modalidad del deporte del baloncesto que se enfoca en la diversion y el disfrute, mas que en la competencia. En este entorno, los jugadores pueden desarrollar habilidades tecnicas y tacticas, mientras que tambien se enfocan en la socializacion y la relacion con otros jugadores.', NULL),
    (0, 'baloncesto seleccionado', 'Baloncesto', 'seleccionado ', 'El curso de baloncesto seleccionado es un espacio dentro del programa de deporte competitivo de la Universidad del Cauca, disenado para atletas con un alto nivel de habilidad y dedicacion. En este entorno, los estudiantes seleccionados tienen la oportunidad de entrenar y competir en eventos deportivos universitarios de caracter local, nacional e internacional.', NULL),
    (0, 'ping pong',            'Ping pong',  'Recreativo',    'El ping pong recreativo es una modalidad del deporte del ping pong que se enfoca en la diversion y el disfrute, mas que en la competencia. En este entorno, los jugadores pueden desarrollar habilidades tecnicas y tacticas, mientras que tambien se enfocan en la socializacion y la relacion con otros jugadores.', NULL);

INSERT INTO tbl_instructor (META_ELIMINADO, PERF_ID) VALUES (0, '2'), (0, '1061813673');
-- NOTA (resuelto, SCRUM-166): el instructor huerfano PERF_ID '987445' del DDL de Oracle
-- nunca se inserta aqui -- no tiene perfil padre y no existe en la BD MySQL ni en el
-- backup previo a la migracion. Para agregarlo, crear primero su tbl_perfil.

INSERT INTO tbl_coordinador (META_ELIMINADO, PERF_ID) VALUES (0, '1');

-- tbl_escenario: ESC_ID omitido, AUTO_INCREMENT asigna valores 1-10
INSERT INTO tbl_escenario (META_ELIMINADO, ESC_NOMBRE, ESC_DESCRIPCION, ESC_NUM_TRIBUNAS, ESC_DISPONIBLE) VALUES
    (0, 'Piscina Olimpica',                    'Natacion. Seccion de clavados fuera de servicio.',                              0, 1),
    (0, 'Pista Atletica',                      'Medidas reglamentarias.',                                                       1, 1),
    (0, 'Cancha Descubierta de Voleibol',      'Medidas reglamentarias.',                                                       1, 1),
    (0, 'Cancha Polifuncional Descubierta',    'Baloncesto, futbol de salon, voleibol.',                                        1, 1),
    (0, 'Coliseo Cubierto Universitario',      'Baloncesto, futbol de salon, futbol sala, voleibol, actividades culturales.',   3, 1),
    (0, 'Patinodromo',                         'Medidas reglamentarias.',                                                       0, 1),
    (0, 'Dojos Cubiertos',                     'Karate do, aikido, taekwondo.',                                                 0, 1),
    (0, 'Sala de Ajedrez',                     NULL,                                                                            0, 1),
    (0, 'Salon de Aerobicos y Baile Deportivo', NULL,                                                                           0, 1),
    (0, 'Sala de Ping Pong',                   NULL,                                                                            0, 1);

