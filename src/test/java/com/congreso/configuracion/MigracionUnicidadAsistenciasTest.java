package com.congreso.configuracion;

import com.congreso.modelo.Asistencia;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MigracionUnicidadAsistenciasTest {

    private JdbcTemplate jdbc;
    private MigracionUnicidadAsistencias migracion;

    @BeforeEach
    void configurar() {
        jdbc = mock(JdbcTemplate.class);
        migracion = new MigracionUnicidadAsistencias(jdbc);
        when(jdbc.queryForObject(contains("migraciones_aplicadas WHERE id"), eq(Integer.class), any()))
                .thenReturn(0);
        when(jdbc.queryForObject(contains("pg_constraint"), eq(Integer.class), any()))
                .thenReturn(0);
    }

    @Test
    void aplicaLaRestriccionCuandoNoHayDuplicados() {
        when(jdbc.queryForObject(contains("HAVING COUNT(*) > 1"), eq(Long.class))).thenReturn(0L);

        migracion.run(null);

        verify(jdbc).execute("ALTER TABLE asistencias "
                + "ADD CONSTRAINT uk_asistencia_asistente_actividad UNIQUE (codigo_asistente, codigo_actividad)");
        verify(jdbc).update(contains("INSERT INTO migraciones_aplicadas"),
                eq("A07-uk-asistencia-par"), any(), eq("aplicada"));
    }

    @Test
    void noDescartaFilasCuandoHayDuplicadosPrevios() {
        when(jdbc.queryForObject(contains("HAVING COUNT(*) > 1"), eq(Long.class))).thenReturn(3L);

        migracion.run(null);

        verify(jdbc, never()).execute(contains("ADD CONSTRAINT"));
        verify(jdbc).update(contains("INSERT INTO migraciones_aplicadas"),
                eq("A07-uk-asistencia-par"), any(), eq("omitida: duplicados=3"));
    }

    @Test
    void esIdempotenteSiYaEstaRegistradaOExisteLaRestriccion() {
        when(jdbc.queryForObject(contains("migraciones_aplicadas WHERE id"), eq(Integer.class), any()))
                .thenReturn(1);
        migracion.run(null);
        verify(jdbc, never()).execute(contains("ADD CONSTRAINT"));

        when(jdbc.queryForObject(contains("migraciones_aplicadas WHERE id"), eq(Integer.class), any()))
                .thenReturn(0);
        when(jdbc.queryForObject(contains("pg_constraint"), eq(Integer.class), any()))
                .thenReturn(1);
        migracion.run(null);
        verify(jdbc, never()).execute(contains("ADD CONSTRAINT"));
    }

    @Test
    void noRompeElArranqueSiFallaLaBase() {
        doThrow(new IllegalStateException("sin permisos"))
                .when(jdbc).execute(contains("CREATE TABLE IF NOT EXISTS"));

        assertDoesNotThrow(() -> migracion.run(null));
    }

    @Test
    void laEntidadDeclaraLaRestriccionUnica() {
        Table tabla = Asistencia.class.getAnnotation(Table.class);

        assertEquals("asistencias", tabla.name());
        assertEquals(1, tabla.uniqueConstraints().length);
        UniqueConstraint restriccion = tabla.uniqueConstraints()[0];
        assertEquals("uk_asistencia_asistente_actividad", restriccion.name());
        assertEquals(2, restriccion.columnNames().length);
    }
}
