package com.congreso.configuracion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ejecutar explícitamente con -Dtest=MigracionUnicidadPostgresIT; requiere PostgreSQL local. */
@SpringBootTest
class MigracionUnicidadPostgresIT {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void laRestriccionUnicaQuedoRegistradaEnPostgres() {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = ?", Integer.class,
                "uk_detalle_inscripcion_par");
        assertEquals(1, total);

        String detalle = jdbcTemplate.queryForObject(
                "SELECT detalle FROM migraciones_aplicadas WHERE id = ?", String.class,
                "C04-uk-detalle-inscripcion");
        assertTrue(detalle.startsWith("aplicada") || detalle.startsWith("omitida: la restriccion ya existia"));
    }
}
