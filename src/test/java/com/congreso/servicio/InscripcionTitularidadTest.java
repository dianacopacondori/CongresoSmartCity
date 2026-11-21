package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.modelo.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class InscripcionTitularidadTest {
    private InscripcionDAO inscripciones;
    private AsistenteDAO asistentes;
    private ActividadDAO actividades;
    private InscripcionServicio servicio;
    private Asistente asistente;
    private Inscripcion inscripcion;

    @BeforeEach
    void preparar() {
        inscripciones = mock(InscripcionDAO.class);
        asistentes = mock(AsistenteDAO.class);
        actividades = mock(ActividadDAO.class);
        servicio = new InscripcionServicio(inscripciones, asistentes, actividades);
        asistente = new Asistente();
        asistente.setCodigo(1L);
        asistente.setRol(RolUsuario.ASISTENTE);
        asistente.setCorreo("propia@example.invalid");
        inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.setCodigo(10L);
        when(asistentes.findByCorreo("propia@example.invalid")).thenReturn(Optional.of(asistente));
        when(inscripciones.findById(10L)).thenReturn(Optional.of(inscripcion));
    }

    @Test
    void noPuedeModificarInscripcionAjenaNiConsultarActividad() {
        Asistente otra = new Asistente();
        otra.setCodigo(2L);
        inscripcion.setAsistente(otra);
        assertThrows(AccessDeniedException.class,
                () -> servicio.inscribirActividad("propia@example.invalid", 10L, 30L));
        verifyNoInteractions(actividades);
        verify(inscripciones, never()).saveAndFlush(any());
    }

    @Test
    void correoInexistenteOSuspendidoNoPuedeAgregarActividades() {
        assertThrows(AccessDeniedException.class,
                () -> servicio.inscribirActividad("otro@example.invalid", 10L, 30L));
        asistente.setEstado(com.congreso.modelo.enums.EstadoUsuario.SUSPENDIDO);
        assertThrows(AccessDeniedException.class,
                () -> servicio.inscribirActividad("propia@example.invalid", 10L, 30L));
        verify(inscripciones, never()).findById(any());
        verifyNoInteractions(actividades);
    }

    @Test
    void inscripcionPendienteNoConsumeCupoNiMuestraExito() {
        assertThrows(IllegalStateException.class,
                () -> servicio.inscribirActividad("propia@example.invalid", 10L, 30L));
        verifyNoInteractions(actividades);
        verify(inscripciones, never()).saveAndFlush(any());
    }

    @Test
    void propiaConfirmadaPermiteAgregarActividadActiva() {
        inscripcion.confirmar();
        Actividad actividad = mock(Actividad.class);
        when(actividad.getEstado()).thenReturn(EstadoActividad.ACTIVA);
        when(actividad.hayCupoDisponible()).thenReturn(true);
        when(actividades.findById(30L)).thenReturn(Optional.of(actividad));

        servicio.inscribirActividad("propia@example.invalid", 10L, 30L);

        assertEquals(1, inscripcion.getActividades().size());
        verify(inscripciones).saveAndFlush(inscripcion);
        verify(actividad).incrementarInscritos();
    }

    @Test
    void actividadCanceladaNoSeAgrega() {
        inscripcion.confirmar();
        Actividad actividad = mock(Actividad.class);
        when(actividad.getEstado()).thenReturn(EstadoActividad.CANCELADA);
        when(actividades.findById(30L)).thenReturn(Optional.of(actividad));

        assertThrows(IllegalStateException.class,
                () -> servicio.inscribirActividad("propia@example.invalid", 10L, 30L));
        verify(inscripciones, never()).saveAndFlush(any());
    }
}
