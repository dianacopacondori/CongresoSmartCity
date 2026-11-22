package com.congreso.configuracion;

import com.congreso.modelo.Inscripcion;
import jakarta.persistence.JoinTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MigracionUnicidadDetalleInscripcionTest {

    private JdbcTemplate jdbc;
    private MigracionUnicidadDetalleInscripcion migracion;

    @BeforeEach
    void configurar() {
        jdbc = mock(JdbcTemplate.class);
        migracion = new MigracionUnicidadDetalleInscripcion(jdbc);
        when(jdbc.queryForObject(contains("migraciones_aplicadas WHERE id"), eq(Integer.class), any()))
                .thenReturn(0);
        when(jdbc.queryForObject(contains("pg_constraint"), eq(Integer.class), any()))
                .thenReturn(0);
    }

    @Test
    void aplicaLaRestriccionCuandoNoHayDuplicados() {
        when(jdbc.queryForObject(contains("HAVING COUNT(*) > 1"), eq(Long.class))).thenReturn(0L);

        migracion.run(null);

        verify(jdbc).execute("ALTER TABLE detalle_inscripcion "
                + "ADD CONSTRAINT uk_detalle_inscripcion_par UNIQUE (codigo_inscripcion, codigo_actividad)");
        verify(jdbc).update(contains("INSERT INTO migraciones_aplicadas"),
                eq("C04-uk-detalle-inscripcion"), any(), eq("aplicada"));
    }

    @Test
    void noDescartaFilasCuandoHayDuplicadosPrevios() {
        when(jdbc.queryForObject(contains("HAVING COUNT(*) > 1"), eq(Long.class))).thenReturn(2L);

        migracion.run(null);

        verify(jdbc, never()).execute(contains("ADD CONSTRAINT"));
        verify(jdbc).update(contains("INSERT INTO migraciones_aplicadas"),
                eq("C04-uk-detalle-inscripcion"), any(), eq("omitida: duplicados=2"));
    }

    @Test
    void esIdempotenteSiYaEstaRegistrada() {
        when(jdbc.queryForObject(contains("migraciones_aplicadas WHERE id"), eq(Integer.class), any()))
                .thenReturn(1);

        migracion.run(null);

        verify(jdbc, never()).execute(contains("ADD CONSTRAINT"));
        verify(jdbc, never()).update(contains("INSERT INTO migraciones_aplicadas"),
                any(), any(), any(), any());
    }

    @Test
    void noRompeElArranqueSiFallaLaBase() {
        doThrow(new IllegalStateException("sin permisos de DDL"))
                .when(jdbc).execute(contains("CREATE TABLE IF NOT EXISTS"));

        assertDoesNotThrow(() -> migracion.run(null));
    }

    @Test
    void laEntidadDeclaraLaRestriccionUnicaParaEsquemasNuevos() throws Exception {
        JoinTable joinTable = Inscripcion.class.getDeclaredField("actividades").getAnnotation(JoinTable.class);

        assertEquals("detalle_inscripcion", joinTable.name());
        assertEquals(1, joinTable.uniqueConstraints().length);
        assertEquals("uk_detalle_inscripcion_par", joinTable.uniqueConstraints()[0].name());
    }
}
