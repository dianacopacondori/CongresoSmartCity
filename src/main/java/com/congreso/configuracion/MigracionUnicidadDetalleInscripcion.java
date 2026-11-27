package com.congreso.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Migración controlada C04: garantiza la unicidad de (inscripción, actividad) en
 * detalle_inscripcion sin borrar filas. Si existen duplicados previos, deja constancia
 * y no aplica la restricción para no descartar datos ajenos.
 *
 * La migración es idempotente: se registra en migraciones_aplicadas y comprueba
 * también que la restricción no exista ya en PostgreSQL.
 */
@Component
public class MigracionUnicidadDetalleInscripcion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionUnicidadDetalleInscripcion.class);
    private static final String ID = "C04-uk-detalle-inscripcion";
    private static final String RESTRICCION = "uk_detalle_inscripcion_par";

    private final JdbcTemplate jdbcTemplate;

    public MigracionUnicidadDetalleInscripcion(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS migraciones_aplicadas ("
                    + "id VARCHAR(80) PRIMARY KEY, aplicada_en TIMESTAMP NOT NULL, detalle VARCHAR(255))");

            Integer aplicadas = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM migraciones_aplicadas WHERE id = ?", Integer.class, ID);
            if (aplicadas != null && aplicadas > 0) {
                return;
            }

            if (existeRestriccion()) {
                registrar("omitida: la restriccion ya existia");
                return;
            }

            Long duplicados = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM (SELECT 1 FROM detalle_inscripcion "
                            + "GROUP BY codigo_inscripcion, codigo_actividad HAVING COUNT(*) > 1) d",
                    Long.class);
            if (duplicados != null && duplicados > 0) {
                log.warn("C04: hay {} pares duplicados en detalle_inscripcion; "
                        + "no se aplica la restriccion UNIQUE para no descartar filas. "
                        + "Regulariza los duplicados y reinicia la aplicacion.", duplicados);
                registrar("omitida: duplicados=" + duplicados);
                return;
            }

            jdbcTemplate.execute("ALTER TABLE detalle_inscripcion "
                    + "ADD CONSTRAINT " + RESTRICCION + " UNIQUE (codigo_inscripcion, codigo_actividad)");
            registrar("aplicada");
            log.info("C04: restriccion UNIQUE {} creada en detalle_inscripcion", RESTRICCION);
        } catch (Exception e) {
            log.error("C04: no se pudo comprobar/aplicar la restriccion UNIQUE de detalle_inscripcion", e);
        }
    }

    private boolean existeRestriccion() {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = ?", Integer.class, RESTRICCION);
        return total != null && total > 0;
    }

    private void registrar(String detalle) {
        jdbcTemplate.update("INSERT INTO migraciones_aplicadas (id, aplicada_en, detalle) "
                        + "VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",
                ID, LocalDateTime.now(), detalle);
    }
}
