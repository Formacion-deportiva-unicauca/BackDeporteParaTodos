-- ============================================================
-- Migracion: 2026-11-02_perfil_por_tipo_vinculo.sql
-- Perfil segun el tipo de vinculo (Fase 1 del modulo de inscripciones).
--
-- Que hace:
--   1) tbl_alumno.ALM_DEPENDENCIA VARCHAR(150) NULL: dependencia, oficina o
--      unidad de Administrativos y Contratistas.
--   2) Amplia CKC_ALM_TIPO_ALUM para aceptar 'Contratista'
--      (Estudiante, Docente, Administrativo, Contratista).
--   3) Carga las facultades de la Universidad del Cauca del formulario de
--      Funcionarios 2026-2 y los programas de pregrado y posgrado de cada facultad.
--
-- Catalogo: el registro de Estudiantes y Docentes exige elegir un programa de su facultad. Los programas
-- se tomaron de las paginas oficiales (ver seccion 4); si falta alguno, agregarlo con un INSERT en tbl_programa.
--
-- Datos existentes: ninguna fila cambia. ALM_DEPENDENCIA queda NULL y los tipos
-- actuales siguen siendo validos.
--
-- Quien lo ejecuta: una persona, manualmente, una vez por base de datos
-- (ddl-auto=none). Requiere MySQL 8.0.16+ (CHECK se aplica desde esa version).
-- Ejemplo: mysql --default-character-set=utf8mb4 -h<host> -P<puerto> -u<usuario> -p <basededatos> < db/migraciones/2026-11-02_perfil_por_tipo_vinculo.sql
-- (los nombres llevan tildes: el archivo es UTF-8, por eso el juego de caracteres utf8mb4).
--
-- IDEMPOTENTE: la columna y el CHECK se verifican en INFORMATION_SCHEMA; las
-- facultades y programas usan INSERT IGNORE (la clave primaria evita duplicados).
-- Correrlo dos veces no falla ni cambia nada.
-- ============================================================

-- 1) tbl_alumno.ALM_DEPENDENCIA
SET @columna_existe = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tbl_alumno'
      AND COLUMN_NAME = 'ALM_DEPENDENCIA'
);
SET @sql_alter = IF(
    @columna_existe = 0,
    'ALTER TABLE tbl_alumno ADD COLUMN ALM_DEPENDENCIA VARCHAR(150) NULL',
    'SELECT 1'
);
PREPARE stmt_alter FROM @sql_alter;
EXECUTE stmt_alter;
DEALLOCATE PREPARE stmt_alter;

-- 2) CKC_ALM_TIPO_ALUM con 'Contratista': se reemplaza solo si todavia no lo incluye.
SET @check_actualizado = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'CKC_ALM_TIPO_ALUM'
      AND CHECK_CLAUSE LIKE '%Contratista%'
);
SET @check_existe = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND CONSTRAINT_NAME = 'CKC_ALM_TIPO_ALUM'
);
SET @sql_drop = IF(
    @check_actualizado = 0 AND @check_existe > 0,
    'ALTER TABLE tbl_alumno DROP CHECK CKC_ALM_TIPO_ALUM',
    'SELECT 1'
);
PREPARE stmt_drop FROM @sql_drop;
EXECUTE stmt_drop;
DEALLOCATE PREPARE stmt_drop;

SET @sql_add = IF(
    @check_actualizado = 0,
    'ALTER TABLE tbl_alumno ADD CONSTRAINT CKC_ALM_TIPO_ALUM CHECK (ALM_TIPO IN (''Estudiante'', ''Docente'', ''Administrativo'', ''Contratista''))',
    'SELECT 1'
);
PREPARE stmt_add FROM @sql_add;
EXECUTE stmt_add;
DEALLOCATE PREPARE stmt_add;

-- 3) Facultades del formulario de Funcionarios 2026-2
INSERT IGNORE INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'Facultad de Artes');
INSERT IGNORE INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'Facultad de Ciencias Agrarias');
INSERT IGNORE INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, 'Facultad de Ingeniería Electrónica y Telecomunicaciones');

-- 4) Programas academicos por facultad (pregrado y posgrado).
--    Fuente: portal institucional de la Universidad del Cauca, consultado el 2026-11-02. Pregrado: listado de programas de
--    la DARCA (51 entradas, 47 nombres unicos al unir sedes) mas Ingenieria Forestal, que figura en la oferta de admision 2026-2.
--    Posgrado: pagina de cada facultad en Programas de posgrado.
--    El PK de tbl_programa es PRG_NOMBRE: un programa dictado en varias sedes (Popayan, Santander de Quilichao,
--    Pasto) queda en una sola fila. Revisar con el cliente los puntos de "Pendientes del catalogo" de abajo.
--
-- Pendientes del catalogo (no cargados o por confirmar):
--   * Maestria en Gestion del Conocimiento y la Innovacion y Maestria en Economia Regional y Politicas Publicas:
--     aparecen en el listado de posgrados ofertados pero no en la pagina de ninguna facultad. No se cargaron.
--   * Maestria en Ingenieria Hidraulica e Hidrologica, Maestria en Ingenieria Ambiental y Doctorado en Ingenieria -
--     Ingenieria Civil e Ingenieria Ambiental: sin facultad en el listado; se asignaron a Ingenieria Civil por su tema.
--   * Nombres con variantes entre paginas (se uso el de la pagina de la facultad): "Especializacion en Derecho Procesal"
--     (ofertados) vs "Derecho Procesal Penal"; Licenciatura en Etnoeducacion vs "...y Ciencias Sociales";
--     Licenciatura en Educacion Basica Primaria vs "...Normalistas".
--   * "Maestria en Revitalizacion y Ensenanza de Lenguas Indigena" se cargo tal como aparece en la fuente.
--   * Programas no ofertados actualmente que no figuren en la pagina de su facultad no estan en este catalogo.

-- Pregrado
-- Facultad de Artes
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Artes Plásticas', 'Facultad de Artes');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Diseño Gráfico', 'Facultad de Artes');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Dirección de Banda', 'Facultad de Artes');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Licenciatura en Música', 'Facultad de Artes');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Música Instrumental', 'Facultad de Artes');
-- Facultad de Ciencias Agrarias
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería Agroindustrial', 'Facultad de Ciencias Agrarias');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería Agropecuaria', 'Facultad de Ciencias Agrarias');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería Forestal', 'Facultad de Ciencias Agrarias');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Tecnología Agroindustrial', 'Facultad de Ciencias Agrarias');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería en Agroecología', 'Facultad de Ciencias Agrarias');
-- Facultad de Ciencias Contables, Económicas y Administrativas
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Administración de Empresas', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Contaduría Pública', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Economía', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Turismo', 'Facultad de Ciencias Contables, Económicas y Administrativas');
-- Facultad de Ciencias de la Salud
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Enfermería', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Fisioterapia', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Fonoaudiología', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Medicina', 'Facultad de Ciencias de la Salud');
-- Facultad de Ciencias Humanas y Sociales
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Antropología', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Filosofía', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Geografía del Desarrollo Regional y Ambiental', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Historia', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Licenciatura en Lingüística y Semiótica', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Licenciatura en Literatura y Lengua Castellana', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Licenciatura en Lenguas Modernas con Énfasis en Inglés y Francés', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Licenciatura en Etnoeducación', 'Facultad de Ciencias Humanas y Sociales');
-- Facultad de Ciencias Naturales, Exactas y de la Educación
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Biología', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Química', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería Física', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Licenciatura en Ciencias Naturales y Educación Ambiental', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Licenciatura en Educación Artística', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Licenciatura en Educación Básica Primaria', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Licenciatura en Educación Física, Recreación y Deportes', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Licenciatura en Matemáticas', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Matemáticas', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
-- Facultad de Derecho, Ciencias Políticas y Sociales
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ciencia Política', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Comunicación Social', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Derecho', 'Facultad de Derecho, Ciencias Políticas y Sociales');
-- Facultad de Ingeniería Civil
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Arquitectura', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería Civil', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería Ambiental', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Geotecnología', 'Facultad de Ingeniería Civil');
-- Facultad de Ingeniería Electrónica y Telecomunicaciones
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería Electrónica y Telecomunicaciones', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería de Sistemas', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería en Automática Industrial', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Tecnología en Telemática', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Ingeniería en Inteligencia Artificial y Ciencia de Datos', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');

-- Posgrado
-- Facultad de Artes
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Música', 'Facultad de Artes');
-- Facultad de Ciencias Agrarias
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Agroindustria', 'Facultad de Ciencias Agrarias');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Gestión Integral del Recurso Hídrico', 'Facultad de Ciencias Agrarias');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Sanidad en Producción Agraria Integrada', 'Facultad de Ciencias Agrarias');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ciencias Agrarias', 'Facultad de Ciencias Agrarias');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ciencias Agroindustriales', 'Facultad de Ciencias Agrarias');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Ciencias Agrarias y Agroindustriales', 'Facultad de Ciencias Agrarias');
-- Facultad de Ciencias Contables, Económicas y Administrativas
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Contabilidad Pública', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Gerencia de Impuestos', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Gerencia de Proyectos', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Mercadeo Corporativo', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Revisoría Fiscal y Auditoría Internacional', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Sistemas Integrados de Calidad', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Contabilidad y Finanzas', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Cooperación Internacional convenio Fundación Norte Sur-Universidad del Cauca', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Estudios Interdisciplinarios del Desarrollo', 'Facultad de Ciencias Contables, Económicas y Administrativas');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Gestión de Organizaciones y Proyectos', 'Facultad de Ciencias Contables, Económicas y Administrativas');
-- Facultad de Ciencias de la Salud
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Desarrollo Humano y Salud', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Anestesiología', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Pediatría', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Ginecología y Obstetricia', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Cirugía General', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Medicina Interna', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Medicina Familiar', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Intervención del Lenguaje Infantil', 'Facultad de Ciencias de la Salud');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Seguridad y Salud en el Trabajo', 'Facultad de Ciencias de la Salud');
-- Facultad de Ciencias Humanas y Sociales
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Estudios de Riesgos de Desastres', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Estudios Interculturales', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Antropología', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Artes Integradas con el Ambiente', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ciencias Humanas', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Estudios de Riesgos de Desastre y Ordenamiento Territorial', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Estudios Interculturales', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ética y Filosofía Política', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Historia', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Revitalización y Enseñanza de Lenguas Indígena', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Antropología', 'Facultad de Ciencias Humanas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Ciencias Humanas', 'Facultad de Ciencias Humanas y Sociales');
-- Facultad de Ciencias Naturales, Exactas y de la Educación
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Actividad Física para la Salud', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Educación Comunitaria', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Pedagogía de la Lectura y la Escritura', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Educación Matemática', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Educación Multicultural', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Educación y Discapacidad', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Entrenamiento Deportivo', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Biología', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Bioingeniería', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ciencias Matemáticas', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ciencias Químicas', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Deporte y Actividad Física', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Educación', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Educación modalidad Profundización', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Educación Popular', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Educación, Estudios del Cuerpo y la Motricidad', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ingeniería Física', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Recursos Hidrobiológicos Continentales', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Ciencias Ambientales', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Ciencias de la Educación', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Ciencias Químicas', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Etnobiología y Estudios Bioculturales', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Ciencias Matemáticas', 'Facultad de Ciencias Naturales, Exactas y de la Educación');
-- Facultad de Derecho, Ciencias Políticas y Sociales
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Derecho Administrativo', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Derecho Contractual Privado', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Derecho de Familia', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Derecho Procesal Penal', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Gobierno y Políticas Públicas', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Periodismo', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Comunicación y Procesos Organizativos', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Derecho Administrativo', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Derechos Humanos y Políticas Públicas para la Convivencia', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Filosofía del Derecho', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Gestión y Desarrollo Social', 'Facultad de Derecho, Ciencias Políticas y Sociales');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Gobierno y Políticas Públicas', 'Facultad de Derecho, Ciencias Políticas y Sociales');
-- Facultad de Ingeniería Civil
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Estructuras', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Ingeniería de la Construcción', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Ingeniería de Recursos Hídricos', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en Ingeniería de Vías Terrestres', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ingeniería de la Construcción', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Geomática', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ingeniería de Pavimentos', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ingeniería de Tránsito', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ingeniería de Vías Terrestres', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ingeniería Hidráulica e Hidrológica', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ingeniería Ambiental', 'Facultad de Ingeniería Civil');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Ingeniería – Ingeniería Civil e Ingeniería Ambiental', 'Facultad de Ingeniería Civil');
-- Facultad de Ingeniería Electrónica y Telecomunicaciones
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Ciencias de la Computación', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Ciencias de la Electrónica', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Doctorado en Ingeniería Telemática', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Automática', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Computación', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Electrónica y Telecomunicaciones', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Ingeniería Telemática', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Maestría en Telecomunicaciones', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
INSERT IGNORE INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, 'Especialización en TIC para la Innovación Educativa', 'Facultad de Ingeniería Electrónica y Telecomunicaciones');
