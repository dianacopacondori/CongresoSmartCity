ALTER TABLE notificaciones ADD COLUMN IF NOT EXISTS evento_origen VARCHAR(80);

CREATE TABLE IF NOT EXISTS notificaciones_enviadas (
    codigo BIGSERIAL PRIMARY KEY,
    destinatario VARCHAR(254) NOT NULL,
    asunto VARCHAR(200) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha TIMESTAMP NOT NULL,
    evento_origen VARCHAR(80) NOT NULL
);
