-- Diagnostico (SOLO LECTURA): fechas de un grupo que tienen a la vez una novedad activa (clase no realizada) y asistencia.
-- No modifica datos. Ejecutar con: USE <tu_base>;   (la conexion debe usar utf8mb4)
-- Una fecha asi es inconsistente: una clase no realizada no puede tener asistencia. Desde la regla nueva el sistema ya no deja
-- crearlas, pero pueden existir de antes. Que hacer con cada una (borrar la novedad o la asistencia) lo decide el Coordinador.

-- 1) Una fila por (grupo, fecha) inconsistente, con cuanta asistencia hay.
SELECT n.CAT_TITULO AS categoria, n.CUR_NOMBRE AS curso, n.GRP_ANIO AS anio, n.GRP_ITERABLE AS iterable, n.NOV_FECHA AS fecha,
       n.NOV_CODIGO AS codigo_novedad, n.NOV_MOTIVO AS motivo, n.NOV_FECHA_CREACION AS novedad_creada_en,
       c.CLS_CODIGO AS codigo_clase, c.CLS_ASISTENCIA_CONFIRMADA_EN AS asistencia_confirmada_en,
       COALESCE(SUM(a.ASI_ESTADO = 'PRESENTE'), 0) AS presentes,
       COALESCE(SUM(a.ASI_ESTADO = 'AUSENTE'), 0)  AS ausentes
FROM tbl_novedad_clase n
JOIN tbl_clase c
  ON c.CAT_TITULO = n.CAT_TITULO AND c.CUR_NOMBRE = n.CUR_NOMBRE AND c.GRP_ANIO = n.GRP_ANIO
 AND c.GRP_ITERABLE = n.GRP_ITERABLE AND c.CLS_FECHA = n.NOV_FECHA AND c.META_ELIMINADO = 0
LEFT JOIN tbl_asistencia a ON a.CLS_CODIGO = c.CLS_CODIGO AND a.META_ELIMINADO = 0
WHERE n.META_ELIMINADO = 0
GROUP BY n.NOV_CODIGO, n.CAT_TITULO, n.CUR_NOMBRE, n.GRP_ANIO, n.GRP_ITERABLE, n.NOV_FECHA, n.NOV_MOTIVO, n.NOV_FECHA_CREACION,
         c.CLS_CODIGO, c.CLS_ASISTENCIA_CONFIRMADA_EN
HAVING presentes + ausentes > 0 OR c.CLS_ASISTENCIA_CONFIRMADA_EN IS NOT NULL
ORDER BY n.NOV_FECHA DESC, n.CAT_TITULO, n.CUR_NOMBRE;

-- 2) Cuantas atenciones (PRESENTE) de esas fechas estan hoy sumando en el reporte mensual (el reporte no descarta las clases con novedad).
SELECT COUNT(*) AS atenciones_presentes_en_fechas_con_novedad
FROM tbl_novedad_clase n
JOIN tbl_clase c
  ON c.CAT_TITULO = n.CAT_TITULO AND c.CUR_NOMBRE = n.CUR_NOMBRE AND c.GRP_ANIO = n.GRP_ANIO
 AND c.GRP_ITERABLE = n.GRP_ITERABLE AND c.CLS_FECHA = n.NOV_FECHA AND c.META_ELIMINADO = 0
JOIN tbl_asistencia a ON a.CLS_CODIGO = c.CLS_CODIGO AND a.META_ELIMINADO = 0 AND a.ASI_ESTADO = 'PRESENTE'
WHERE n.META_ELIMINADO = 0;
