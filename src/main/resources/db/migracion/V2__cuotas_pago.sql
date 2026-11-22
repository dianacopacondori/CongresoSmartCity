-- C05: historial individual de cuotas de cada pago simulado.
-- Equivalente a schema.sql; se ejecuta automaticamente antes de Hibernate.
CREATE TABLE IF NOT EXISTS cuotas_pago (
    codigo BIGSERIAL PRIMARY KEY,
    codigo_pago BIGINT NOT NULL REFERENCES pagos(codigo_pago),
    numero_cuota INTEGER NOT NULL,
    importe DOUBLE PRECISION NOT NULL,
    fecha_registro DATE NOT NULL,
    observaciones VARCHAR(255),
    CONSTRAINT uk_cuotas_pago_numero UNIQUE (codigo_pago, numero_cuota)
);
