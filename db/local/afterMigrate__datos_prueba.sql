-- Datos de prueba, solo entorno local. Tomados tal cual de src/main/resources/DDL-MYSQL.sql
-- (lineas 538-742,747), que no estan en db/flyway/V2__datos_catalogo.sql ni en db/ci.
-- INSERT IGNORE: re-ejecutar este script no falla ni duplica filas.
INSERT IGNORE INTO tbl_perfil (META_ELIMINADO, PERF_ID, PERF_NOMBRE, PERF_CORREO, PERF_IMAGEN, PERF_TIPOID, PERF_SEXO) VALUES
    (0, '3',          'SMERT EMJ',                       'edynsonjm@gmail.com',          NULL, 'CC', 'M'),
    (0, '1',          'EDYNSON MUNOZ JIMENEZ',           'edinsonmjim@unicauca.edu.co',  NULL, 'CC', 'M'),
    (0, '2',          'edynson munoz jimenez',           'edynsonmj@gmail.com',          NULL, 'CC', 'M'),
    (0, '1141315391', 'MIGUEL ANGEL DORADO QUINAYAS',    'migueladorado@unicauca.edu.co', NULL, 'CC', 'M'),
    (0, '1002958703', 'Yonathan Fernandez Guengue',      'tochavi@unicauca.edu.co',      NULL, 'CC', 'M'),
    (0, '1061786866', 'Luis Alexis Mendez Rojas',        'lamendez@unicauca.edu.co',     NULL, 'CC', 'M'),
    (0, '1061700114', 'Mitchelt Hernan Chilito Villota', 'mhchilito@unicauca.edu.co',    NULL, 'CC', 'M'),
    (0, '1061721857', 'JOHAN RICARDO MENDEZ CASTRO',     'johanmendez@unicauca.edu.co',  NULL, 'TI', 'M'),
    (0, '1061813673', 'Juan Sebastian Pisso',            'jpisso@unicauca.edu.co',       NULL, 'CC', 'M');

INSERT IGNORE INTO tbl_alumno (META_ELIMINADO, PERF_ID, ALM_CODIGO, ALM_TIPO, ALM_ESTADO) VALUES
    (0, '3',          NULL, 'Estudiante',     NULL),
    (0, '1',          NULL, 'Administrativo', NULL),
    (0, '2',          NULL, 'Docente',        NULL),
    (0, '1141315391', NULL, 'Estudiante',     NULL),
    (0, '1002958703', NULL, 'Estudiante',     NULL),
    (0, '1061786866', NULL, 'Estudiante',     NULL),
    (0, '1061700114', NULL, 'Estudiante',     NULL),
    (0, '1061721857', NULL, 'Estudiante',     NULL);

INSERT IGNORE INTO tbl_instructor (META_ELIMINADO, PERF_ID) VALUES (0, '2'), (0, '1061813673');

INSERT IGNORE INTO tbl_coordinador (META_ELIMINADO, PERF_ID) VALUES (0, '1');

INSERT IGNORE INTO tbl_curso (META_ELIMINADO, CUR_NOMBRE, DEPT_NOMBRE, CAT_TITULO, CUR_DESCRIPCION, CUR_IMAGEN) VALUES
    (0, 'futbol 1',             'Futbol',     'seleccionado ', 'Este curso es un programa competitivo que busca desarrollar las habilidades tecnicas, tacticas y fisicas de los jugadores para enfrentar a otras facultades e instituciones a nivel local y nacional.', NULL),
    (0, 'baloncesto',           'Baloncesto', 'seleccionado ', 'Este curso es ideal para hombres y mujeres que buscan competir a nivel superior en baloncesto. Se enfoca en la practica de tecnicas avanzadas y la mejora de la condicion fisica.', NULL),
    (0, 'natacion',              'Natacion',   'Semillero',     'Descr', NULL),
    (0, 'Baloncesto masculino', 'Baloncesto', 'Recreativo',    'El baloncesto recreativo es una modalidad del deporte del baloncesto que se enfoca en la diversion y el disfrute, mas que en la competencia. En este entorno, los jugadores pueden desarrollar habilidades tecnicas y tacticas, mientras que tambien se enfocan en la socializacion y la relacion con otros jugadores.', NULL),
    (0, 'baloncesto seleccionado', 'Baloncesto', 'seleccionado ', 'El curso de baloncesto seleccionado es un espacio dentro del programa de deporte competitivo de la Universidad del Cauca, disenado para atletas con un alto nivel de habilidad y dedicacion. En este entorno, los estudiantes seleccionados tienen la oportunidad de entrenar y competir en eventos deportivos universitarios de caracter local, nacional e internacional.', NULL),
    (0, 'ping pong',            'Ping pong',  'Recreativo',    'El ping pong recreativo es una modalidad del deporte del ping pong que se enfoca en la diversion y el disfrute, mas que en la competencia. En este entorno, los jugadores pueden desarrollar habilidades tecnicas y tacticas, mientras que tambien se enfocan en la socializacion y la relacion con otros jugadores.', NULL);
