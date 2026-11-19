-- V4: indice por fecha de clase para los reportes mensuales (filtro principal por rango de CLS_FECHA). Solo un indice: no cambia datos.
CREATE INDEX IDX_CLASE_FECHA ON tbl_clase (CLS_FECHA);
