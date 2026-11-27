package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.ColaboradorDAO;
import com.congreso.dao.SalaDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Taller;
import com.congreso.modelo.enums.EstadoActividad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActividadFiltrosServicioTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 26);

    private ActividadServicio servicio;

    @BeforeEach
    void configurar() {
        ActividadDAO actividades = mock(ActividadDAO.class);
        when(actividades.findByEstado(EstadoActividad.ACTIVA)).thenReturn(List.of(
                actividad(new Ponencia(), "Smart Cities y movilidad", FECHA),
                actividad(new Taller(), "Taller de sensores", FECHA),
                actividad(new Ponencia(), "Movilidad electrónica", FECHA.plusDays(1)),
                actividad(new Taller(), "Taller sin cupo", FECHA, 5, 5),
                actividad(new Ponencia(), "Ponenciacancelada", FECHA, 0, 0, EstadoActividad.CANCELADA)));
        servicio = new ActividadServicio(actividades, mock(SalaDAO.class), mock(ColaboradorDAO.class));
    }

    @Test
    void sinFiltrosDevuelveSoloActivasConCupo() {
        assertEquals(List.of("Smart Cities y movilidad", "Taller de sensores", "Movilidad electrónica"),
                titulos(servicio.obtenerActividadesFiltradas(null, null, null)));
    }

    @Test
    void filtraPorNombreIgnorandoMayusculas() {
        assertEquals(List.of("Smart Cities y movilidad", "Movilidad electrónica"),
                titulos(servicio.obtenerActividadesFiltradas("MOVILIDAD", null, null)));
    }

    @Test
    void filtraPorTipo() {
        assertEquals(List.of("Taller de sensores"),
                titulos(servicio.obtenerActividadesFiltradas(null, "Talleres", null)));
        assertEquals(List.of("Smart Cities y movilidad", "Movilidad electrónica"),
                titulos(servicio.obtenerActividadesFiltradas(null, "PONENCIA", null)));
    }

    @Test
    void filtraPorFecha() {
        assertEquals(List.of("Smart Cities y movilidad", "Taller de sensores"),
                titulos(servicio.obtenerActividadesFiltradas(null, null, FECHA)));
    }

    @Test
    void combinaFiltros() {
        assertEquals(List.of("Taller de sensores"),
                titulos(servicio.obtenerActividadesFiltradas("sensores", "TALLER", FECHA)));
        assertEquals(List.of(), titulos(servicio.obtenerActividadesFiltradas("sensores", "PONENCIA", FECHA)));
    }

    @Test
    void tipoNoValidoSeRechazaSinDevolverActividades() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerActividadesFiltradas(null, "OTRO", null));
    }

    private static Actividad actividad(Actividad actividad, String titulo, LocalDate fecha) {
        return actividad(actividad, titulo, fecha, 10, 0, EstadoActividad.ACTIVA);
    }

    private static Actividad actividad(Actividad actividad, String titulo, LocalDate fecha, int cupo, int inscritos) {
        return actividad(actividad, titulo, fecha, cupo, inscritos, EstadoActividad.ACTIVA);
    }

    private static Actividad actividad(Actividad actividad, String titulo, LocalDate fecha,
                                       int cupo, int inscritos, EstadoActividad estado) {
        actividad.setTitulo(titulo);
        actividad.setFecha(fecha);
        actividad.setCupo(cupo);
        actividad.setInscritos(inscritos);
        actividad.setEstado(estado);
        return actividad;
    }

    private static List<String> titulos(List<Actividad> actividades) {
        return actividades.stream().map(Actividad::getTitulo).toList();
    }
}
