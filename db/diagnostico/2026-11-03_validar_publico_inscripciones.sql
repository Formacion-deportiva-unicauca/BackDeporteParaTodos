-- Diagnostico (SOLO LECTURA): quien esta inscrito en cada curso segun su tipo de vinculo y el publico del curso.
-- No modifica datos. Ejecutar con: USE <tu_base>;

-- 1) Publico configurado de cada curso. Si el curso dice MIXTO, el backend deja inscribir a cualquiera.
SELECT CAT_TITULO AS categoria, CUR_NOMBRE AS curso, CUR_PUBLICO AS publico
FROM tbl_curso
WHERE META_ELIMINADO = 0
ORDER BY CAT_TITULO, CUR_NOMBRE;

-- 2) Tipo de vinculo (ALM_TIPO) de cada persona y su(s) rol(es). Un Docente registrado como "Estudiante" aparece aqui.
SELECT p.PERF_ID, p.PERF_NOMBRE, p.PERF_CORREO,
       a.ALM_TIPO AS tipo_vinculo,
       a.ALM_CODIGO AS codigo,
       a.ALM_DEPENDENCIA AS dependencia,
       (SELECT GROUP_CONCAT(r.rol) FROM (
            SELECT PERF_ID, 'Instructor'  AS rol FROM tbl_instructor  WHERE META_ELIMINADO = 0
            UNION ALL
            SELECT PERF_ID, 'Coordinador' AS rol FROM tbl_coordinador WHERE META_ELIMINADO = 0
        ) r WHERE r.PERF_ID = p.PERF_ID) AS roles_de_gestion
FROM tbl_perfil p
JOIN tbl_alumno a ON a.PERF_ID = p.PERF_ID AND a.META_ELIMINADO = 0
WHERE p.META_ELIMINADO = 0
ORDER BY a.ALM_TIPO, p.PERF_NOMBRE;

-- 3) Resumen: cuantas personas hay por tipo de vinculo.
SELECT ALM_TIPO AS tipo_vinculo, COUNT(*) AS personas
FROM tbl_alumno
WHERE META_ELIMINADO = 0
GROUP BY ALM_TIPO;

-- 4) INSCRIPCIONES QUE NO CUMPLEN el publico del curso (activas: sin desvincular).
--    ESTUDIANTE  -> solo ALM_TIPO = 'Estudiante'
--    FUNCIONARIO -> solo Docente, Administrativo o Contratista
SELECT c.CAT_TITULO AS categoria, c.CUR_NOMBRE AS curso, c.CUR_PUBLICO AS publico_curso,
       i.GRP_ANIO AS anio, i.GRP_ITERABLE AS grupo,
       p.PERF_NOMBRE AS persona, p.PERF_CORREO, a.ALM_TIPO AS tipo_vinculo,
       i.INSCR_ESTADO AS estado, i.INSCR_FECHAINSCRIPCION AS fecha_inscripcion
FROM tbl_inscripcion i
JOIN tbl_curso  c ON c.CAT_TITULO = i.CAT_TITULO AND c.CUR_NOMBRE = i.CUR_NOMBRE
JOIN tbl_alumno a ON a.PERF_ID = i.PERF_ID
JOIN tbl_perfil p ON p.PERF_ID = i.PERF_ID
WHERE i.META_ELIMINADO = 0
  AND i.INSCR_FECHADESVINCULACION IS NULL
  AND ( (c.CUR_PUBLICO = 'ESTUDIANTE'  AND a.ALM_TIPO <> 'Estudiante')
     OR (c.CUR_PUBLICO = 'FUNCIONARIO' AND a.ALM_TIPO NOT IN ('Docente', 'Administrativo', 'Contratista')) )
ORDER BY c.CAT_TITULO, c.CUR_NOMBRE, p.PERF_NOMBRE;

-- 5) Detalle del caso reportado: Fulbol 11 y la persona inscrita desde la planilla.
SELECT c.CUR_NOMBRE AS curso, c.CUR_PUBLICO AS publico_curso,
       p.PERF_NOMBRE AS persona, p.PERF_CORREO, a.ALM_TIPO AS tipo_vinculo
FROM tbl_inscripcion i
JOIN tbl_curso  c ON c.CAT_TITULO = i.CAT_TITULO AND c.CUR_NOMBRE = i.CUR_NOMBRE
JOIN tbl_alumno a ON a.PERF_ID = i.PERF_ID
JOIN tbl_perfil p ON p.PERF_ID = i.PERF_ID
WHERE c.CUR_NOMBRE LIKE 'Fulbol%'
  AND i.META_ELIMINADO = 0;
