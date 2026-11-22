package com.congreso.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * A07: garantiza la unicidad efectiva de (asistente, actividad) en asistencias.
 * Si hay duplicados previos no elimina filas: registra el hallazgo y omite la
 * restricción para que un operador regularice los datos.
 */
@Component
public class MigracionUnicidadAsistencias implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionUnicidadAsistencias.class);
    private static final String ID = "A07-uk-asistencia-par";
    private static final String RESTRICCION = "uk_asistencia_asistente_actividad";

    private final JdbcTemplate jdbcTemplate;

    public MigracionUnicidadAsistencias(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS migraciones_aplicadas ("
                    + "id VARCHAR(80) PRIMARY KEY, aplicada_en TIMESTAMP NOT NULL, detalle VARCHAR(255))");

            Integer aplicadas = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM migraciones_aplicadas WHERE id = ?", Integer.class, ID);
            if (aplicadas != null && aplicadas > 0) return;

            Integer existentes = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = ?", Integer.class, RESTRICCION);
            if (existentes != null && existentes > 0) {
                registrar("omitida: la restriccion ya existia");
                return;
            }

            Long duplicados = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM (SELECT 1 FROM asistencias "
                            + "GROUP BY codigo_asistente, codigo_actividad HAVING COUNT(*) > 1) d",
                    Long.class);
            if (duplicados != null && duplicados > 0) {
                log.warn("A07: hay {} pares duplicados en asistencias; no se aplica UNIQUE "
                        + "para no descartar filas. Regulariza y reinicia.", duplicados);
                registrar("omitida: duplicados=" + duplicados);
                return;
            }

            jdbcTemplate.execute("ALTER TABLE asistencias "
                    + "ADD CONSTRAINT " + RESTRICCION
                    + " UNIQUE (codigo_asistente, codigo_actividad)");
            registrar("aplicada");
            log.info("A07: restriccion UNIQUE {} creada en asistencias", RESTRICCION);
        } catch (Exception e) {
            log.error("A07: no se pudo comprobar/aplicar la restriccion UNIQUE de asistencias", e);
        }
    }

    private void registrar(String detalle) {
        jdbcTemplate.update("INSERT INTO migraciones_aplicadas (id, aplicada_en, detalle) "
                        + "VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",
                ID, LocalDateTime.now(), detalle);
    }
}
