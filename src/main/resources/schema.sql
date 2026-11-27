-- C05: tabla de historial de cuotas. Se ejecuta antes de Hibernate (spring.sql.init)
-- para que ddl-auto=validate encuentre la estructura al arrancar.
CREATE TABLE IF NOT EXISTS cuotas_pago (
    codigo BIGSERIAL PRIMARY KEY,
    codigo_pago BIGINT NOT NULL REFERENCES pagos(codigo_pago),
    numero_cuota INTEGER NOT NULL,
    importe DOUBLE PRECISION NOT NULL,
    fecha_registro DATE NOT NULL,
    observaciones VARCHAR(255),
    CONSTRAINT uk_cuotas_pago_numero UNIQUE (codigo_pago, numero_cuota)
);

-- C04: tabla de control de migraciones (la migración en Java también la crea).
CREATE TABLE IF NOT EXISTS migraciones_aplicadas (
    id VARCHAR(80) PRIMARY KEY,
    aplicada_en TIMESTAMP NOT NULL,
    detalle VARCHAR(255)
);

-- A06: registrador de cada asistencia. Nullable para conservar registros históricos.
ALTER TABLE asistencias ADD COLUMN IF NOT EXISTS registrado_por BIGINT REFERENCES usuarios(codigo);

-- A07: auditoría de correcciones. Nullable para conservar registros históricos.
ALTER TABLE asistencias ADD COLUMN IF NOT EXISTS justificacion_correccion VARCHAR(500);
ALTER TABLE asistencias ADD COLUMN IF NOT EXISTS corregido_por BIGINT REFERENCES usuarios(codigo);
ALTER TABLE asistencias ADD COLUMN IF NOT EXISTS fecha_correccion TIMESTAMP;
