-- Deportes y cursos 2026-2
-- Datos de prueba para desarrollo local (oferta real del horario 2026-2, sin datos personales).
-- Idempotente: cada insercion tiene NOT EXISTS, asi que puede ejecutarse en cada migrate.
-- Depende de los catalogos de V2 (categorias de curso) y se ejecuta despues de afterMigrate__datos_prueba.sql.
SET NAMES utf8mb4;
INSERT INTO tbl_deporte (META_ELIMINADO, DEPT_NOMBRE)
SELECT 0, v.n FROM (VALUES ROW('Taekwondo'),ROW('Ajedrez'),ROW('Ritmos latinos'),ROW('Actividad fisica musicalizada'),ROW('Halterofilia'),ROW('Futbol sala'),ROW('Judo'),ROW('Defensa personal')) AS v(n)
WHERE NOT EXISTS (SELECT 1 FROM tbl_deporte d WHERE d.DEPT_NOMBRE = v.n);
INSERT INTO tbl_curso (META_ELIMINADO, CUR_NOMBRE, DEPT_NOMBRE, CAT_TITULO, CUR_DESCRIPCION, CUR_ESTADO_INSCRIPCIONES, CUR_PUBLICO)
SELECT 0, v.n, v.d, v.l, CONCAT('Curso de ', LOWER(v.d), ' (', LOWER(v.l), '), horario 2026-2.'), 'ABIERTO', v.p FROM (VALUES
ROW('Taekwondo recreativo 1','Taekwondo','Recreativo','MIXTO'),
ROW('Taekwondo recreativo 2','Taekwondo','Recreativo','MIXTO'),
ROW('Taekwondo semillero','Taekwondo','Semillero','MIXTO'),
ROW('Taekwondo seleccionado','Taekwondo','seleccionado','MIXTO'),
ROW('Ajedrez preseleccionados','Ajedrez','seleccionado','MIXTO'),
ROW('Ajedrez semillero','Ajedrez','Semillero','MIXTO'),
ROW('Ajedrez funcionarios','Ajedrez','Recreativo','FUNCIONARIO'),
ROW('Tenis de mesa docentes y administrativos','Ping pong','Recreativo','FUNCIONARIO'),
ROW('Tenis de mesa seleccionado','Ping pong','seleccionado','MIXTO'),
ROW('Tenis de mesa recreativo estudiantes','Ping pong','Recreativo','ESTUDIANTE'),
ROW('Tenis de mesa semillero','Ping pong','Semillero','ESTUDIANTE'),
ROW('Bachata básica','Ritmos latinos','Recreativo','MIXTO'),
ROW('Salsa caleña básica','Ritmos latinos','Recreativo','MIXTO'),
ROW('Salsa caleña intermedia','Ritmos latinos','Recreativo','MIXTO'),
ROW('Ritmos latinos representativo','Ritmos latinos','seleccionado','MIXTO'),
ROW('Ritmos latinos semillero','Ritmos latinos','Semillero','MIXTO'),
ROW('Ritmos latinos','Ritmos latinos','Recreativo','MIXTO'),
ROW('Ritmos latinos funcionarios','Ritmos latinos','Recreativo','FUNCIONARIO'),
ROW('Step funcional','Actividad fisica musicalizada','Recreativo','MIXTO'),
ROW('Baile deportivo','Actividad fisica musicalizada','Recreativo','MIXTO'),
ROW('Viejoteca','Actividad fisica musicalizada','Recreativo','MIXTO'),
ROW('Cardio tren superior e inferior','Actividad fisica musicalizada','Recreativo','MIXTO'),
ROW('Natación nivel 1','Natacion','Recreativo','MIXTO'),
ROW('Natación nivel 2','Natacion','Recreativo','MIXTO'),
ROW('Natación nivel 3','Natacion','Recreativo','MIXTO'),
ROW('Natación nivel 4','Natacion','Recreativo','MIXTO'),
ROW('Natación preseleccionado y seleccionado','Natacion','seleccionado','MIXTO'),
ROW('Acondicionamiento físico nivel básico','Halterofilia','Recreativo','MIXTO'),
ROW('Halterofilia semillero','Halterofilia','Semillero','MIXTO'),
ROW('Levantamiento de pesas avanzado','Halterofilia','seleccionado','MIXTO'),
ROW('Futbol sala recreativo femenino','Futbol sala','Recreativo','MIXTO'),
ROW('Futbol sala recreativo masculino','Futbol sala','Recreativo','MIXTO'),
ROW('Futbol sala semillero femenino','Futbol sala','Semillero','MIXTO'),
ROW('Futbol sala semillero masculino','Futbol sala','Semillero','MIXTO'),
ROW('Futbol sala seleccionado femenino','Futbol sala','seleccionado','MIXTO'),
ROW('Futbol sala seleccionado funcionarias','Futbol sala','seleccionado','FUNCIONARIO'),
ROW('Voleibol recreativo','Voleibol','Recreativo','MIXTO'),
ROW('Voleibol semillero','Voleibol','Semillero','MIXTO'),
ROW('Voleibol seleccionado femenino','Voleibol','seleccionado','MIXTO'),
ROW('Voleibol seleccionado masculino','Voleibol','seleccionado','MIXTO'),
ROW('Baloncesto seleccionado 3x3 femenino','Baloncesto','seleccionado','MIXTO'),
ROW('Baloncesto seleccionado 5x5 femenino','Baloncesto','seleccionado','MIXTO'),
ROW('Baloncesto seleccionado femenino administrativas','Baloncesto','seleccionado','FUNCIONARIO'),
ROW('Baloncesto semillero femenino','Baloncesto','Semillero','MIXTO'),
ROW('Baloncesto recreativo femenino','Baloncesto','Recreativo','MIXTO'),
ROW('Futbol seleccion estudiantes','Futbol','seleccionado','ESTUDIANTE'),
ROW('Futbol seleccion funcionarios','Futbol','seleccionado','FUNCIONARIO'),
ROW('Futbol semilleros','Futbol','Semillero','MIXTO'),
ROW('Futbol recreativo estudiantes masculino','Futbol','Recreativo','ESTUDIANTE'),
ROW('Futbol administrativos mayores','Futbol','Recreativo','FUNCIONARIO'),
ROW('Futbol funcionarios mayores','Futbol','Recreativo','FUNCIONARIO'),
ROW('Atletismo seleccionado','Atletismo','seleccionado','MIXTO'),
ROW('Atletismo semillero','Atletismo','Semillero','MIXTO'),
ROW('Atletismo recreativo','Atletismo','Recreativo','MIXTO'),
ROW('Judo','Judo','Recreativo','MIXTO'),
ROW('Defensa personal','Defensa personal','Recreativo','MIXTO')
) AS v(n,d,l,p)
WHERE NOT EXISTS (SELECT 1 FROM tbl_curso c WHERE c.CUR_NOMBRE = v.n AND c.CAT_TITULO = v.l)
AND EXISTS (SELECT 1 FROM tbl_categoria_curso k WHERE k.CAT_TITULO = v.l AND k.META_ELIMINADO = 0);
