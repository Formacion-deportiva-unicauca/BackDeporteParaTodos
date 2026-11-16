-- V3: corrige facultad/programa, cargados en produccion antes de que existiera
-- esta migracion, con doble codificacion (UTF-8 leido como Latin-1 y guardado
-- otra vez como UTF-8; por ejemplo, "Antropologia" quedo mal codificado,
-- mostrando dos caracteres de mas donde deberia ir una sola vocal con tilde).
-- Este archivo es puro ASCII a proposito, incluso en los comentarios: si
-- tuviera tildes podria terminar sufriendo el mismo problema que corrige.
--
-- Deteccion: cada caracter mal codificado deja la secuencia de bytes 0xC3 0x83
-- (HEX 'C383', el primer caracter "A con tilde" de la doble codificacion) en
-- la columna. Un nombre correctamente codificado nunca contiene ese patron.
--
-- Correccion: CONVERT(CAST(CONVERT(x USING latin1) AS BINARY) USING utf8mb4)
-- reconstruye los bytes UTF-8 originales (vuelve a codificar en latin1 los
-- caracteres ya mal decodificados, y los reinterpreta como utf8mb4). Se
-- verifico manualmente sobre los datos reales que esto no deja "?" ni
-- caracteres residuales, y que no produce colisiones de llave ni programas
-- huerfanos (141 programas y 10 facultades distintos tras corregir).
--
-- Cada UPDATE solo toca filas donde HEX(columna) LIKE '%C383%', asi que la
-- migracion es idempotente: una segunda corrida, o un entorno donde el
-- catalogo ya esta bien codificado (como el seed de V2 en local/CI), no
-- cambia nada.
--
-- FAC_NOMBRE es la llave primaria de tbl_facultad y PRG_NOMBRE la de
-- tbl_programa; ambas estan referenciadas por columnas que tambien hay que
-- corregir (tbl_programa.FAC_NOMBRE y tbl_intermedia_alumno_programa.PRG_NOMBRE).
-- Por eso se corrige en el sitio (UPDATE) y nunca con DELETE + INSERT: un
-- DELETE dispararia el ON DELETE CASCADE de tbl_intermedia_alumno_programa y
-- borraria la relacion de los alumnos con su programa. Las FK se desactivan
-- mientras se corrigen las 3 tablas para poder actualizar cada una de forma
-- independiente, sin depender del orden: cada valor danado se corrige con la
-- misma formula sobre su propia columna, sin necesidad de JOIN entre tablas,
-- porque todas sufrieron la misma transformacion sobre el mismo nombre
-- original (no hay que esperar a que la tabla padre ya este corregida).

SET FOREIGN_KEY_CHECKS = 0;

-- tbl_intermedia_alumno_programa.PRG_NOMBRE: NOT NULL (parte de la llave primaria)
UPDATE tbl_intermedia_alumno_programa
SET PRG_NOMBRE = CONVERT(CAST(CONVERT(PRG_NOMBRE USING latin1) AS BINARY) USING utf8mb4)
WHERE HEX(PRG_NOMBRE) LIKE '%C383%';

-- tbl_programa.PRG_NOMBRE: NOT NULL (es la llave primaria)
UPDATE tbl_programa
SET PRG_NOMBRE = CONVERT(CAST(CONVERT(PRG_NOMBRE USING latin1) AS BINARY) USING utf8mb4)
WHERE HEX(PRG_NOMBRE) LIKE '%C383%';

-- tbl_programa.FAC_NOMBRE: si puede ser NULL (ver V1, FK_PROGRAMA_FACULTAD ON
-- DELETE SET NULL), por eso se comprueba IS NOT NULL antes del HEX
UPDATE tbl_programa
SET FAC_NOMBRE = CONVERT(CAST(CONVERT(FAC_NOMBRE USING latin1) AS BINARY) USING utf8mb4)
WHERE FAC_NOMBRE IS NOT NULL AND HEX(FAC_NOMBRE) LIKE '%C383%';

-- tbl_facultad.FAC_NOMBRE: NOT NULL (es la llave primaria)
UPDATE tbl_facultad
SET FAC_NOMBRE = CONVERT(CAST(CONVERT(FAC_NOMBRE USING latin1) AS BINARY) USING utf8mb4)
WHERE HEX(FAC_NOMBRE) LIKE '%C383%';

-- 'fiet' es una facultad de prueba cargada en V2, sin relacion con el problema
-- de codificacion. Se borra solo si ningun programa la referencia todavia; si
-- ya se borro en una corrida anterior, o nunca existio en este entorno, la
-- condicion simplemente no encuentra filas y no pasa nada.
DELETE FROM tbl_facultad
WHERE FAC_NOMBRE = 'fiet'
  AND NOT EXISTS (
      SELECT 1 FROM tbl_programa WHERE tbl_programa.FAC_NOMBRE = 'fiet'
  );

SET FOREIGN_KEY_CHECKS = 1;
