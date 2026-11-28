ALTER TABLE inscripciones
    ADD COLUMN IF NOT EXISTS tarifa NUMERIC(10,2);

UPDATE inscripciones
SET tarifa = CASE
    WHEN LOWER(BTRIM(tipo)) = 'estudiante' THEN 50.00
    WHEN LOWER(BTRIM(tipo)) = 'externo' THEN 100.00
END
WHERE tarifa IS NULL
  AND LOWER(BTRIM(tipo)) IN ('estudiante', 'externo');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM inscripciones WHERE tarifa IS NULL) THEN
        RAISE EXCEPTION 'No se pudo asignar tarifa: hay inscripciones con tipo no reconocido';
    END IF;
END;
$$;

ALTER TABLE inscripciones
    ALTER COLUMN tarifa SET NOT NULL;
