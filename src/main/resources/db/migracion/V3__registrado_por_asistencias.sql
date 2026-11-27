-- A06: trazabilidad del personal que registra la asistencia.
ALTER TABLE asistencias ADD COLUMN IF NOT EXISTS registrado_por BIGINT REFERENCES usuarios(codigo);
