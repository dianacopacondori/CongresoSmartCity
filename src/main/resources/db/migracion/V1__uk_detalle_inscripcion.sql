-- C04: unicidad de (codigo_inscripcion, codigo_actividad) en detalle_inscripcion.
-- Aplicar con un usuario con permisos de DDL. La aplicacion también la ejecuta de forma
-- idempotente al arrancar mediante MigracionUnicidadDetalleInscripcion.

-- 1. Comprobar duplicados previos. Si esta consulta devuelve filas, regularizalas antes
--    de continuar: esta migracion NO elimina datos ajenos.
-- SELECT codigo_inscripcion, codigo_actividad, COUNT(*)
-- FROM detalle_inscripcion
-- GROUP BY codigo_inscripcion, codigo_actividad
-- HAVING COUNT(*) > 1;

CREATE TABLE IF NOT EXISTS migraciones_aplicadas (
    id VARCHAR(80) PRIMARY KEY,
    aplicada_en TIMESTAMP NOT NULL,
    detalle VARCHAR(255)
);

ALTER TABLE detalle_inscripcion
    ADD CONSTRAINT uk_detalle_inscripcion_par
    UNIQUE (codigo_inscripcion, codigo_actividad);

INSERT INTO migraciones_aplicadas (id, aplicada_en, detalle)
VALUES ('C04-uk-detalle-inscripcion', NOW(), 'aplicada manualmente')
ON CONFLICT (id) DO NOTHING;
