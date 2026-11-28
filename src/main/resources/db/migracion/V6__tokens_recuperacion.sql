CREATE TABLE IF NOT EXISTS tokens_recuperacion (
    token UUID PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(codigo),
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_tokens_recuperacion_usuario ON tokens_recuperacion(usuario_id);
