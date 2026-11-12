-- Datos FICTICIOS solo para el CI. El directorio db/ci nunca se monta en produccion.
-- Los tests de integracion asumen un instructor con PERF_ID = '2' (INSTRUCTOR_SEED_ID).
INSERT IGNORE INTO tbl_perfil (META_ELIMINADO, PERF_ID, PERF_NOMBRE, PERF_CORREO, PERF_IMAGEN, PERF_TIPOID, PERF_SEXO)
VALUES (0, '2', 'Instructor de Prueba CI', 'instructor.ci@example.com', NULL, 'CC', 'M');
INSERT IGNORE INTO tbl_instructor (META_ELIMINADO, PERF_ID) VALUES (0, '2');
