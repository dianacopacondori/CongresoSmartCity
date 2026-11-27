-- A07: campos de auditoría para la corrección de asistencias.
-- La restricción UNIQUE se añade mediante MigracionUnicidadAsistencias (detecta duplicados).
ALTER TABLE asistencias ADD COLUMN IF NOT EXISTS justificacion_correccion VARCHAR(500);
ALTER TABLE asistencias ADD COLUMN IF NOT EXISTS corregido_por BIGINT REFERENCES usuarios(codigo);
ALTER TABLE asistencias ADD COLUMN IF NOT EXISTS fecha_correccion TIMESTAMP;
