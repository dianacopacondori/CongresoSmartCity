package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
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
    private PagoDAO pagos;
    private Asistente asistente;
    private Inscripcion inscripcion;

    @BeforeEach
    void preparar() {
        inscripciones = mock(InscripcionDAO.class);
        asistentes = mock(AsistenteDAO.class);
        actividades = mock(ActividadDAO.class);
        pagos = mock(PagoDAO.class);
        servicio = new InscripcionServicio(inscripciones, asistentes, actividades, pagos);
        asistente = new Asistente();
        asistente.setCodigo(1L);
        asistente.setRol(RolUsuario.ASISTENTE);
        asistente.setCorreo("propia@example.invalid");
        inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.setCodigo(10L);
        when(asistentes.findByCorreo("propia@example.invalid")).thenReturn(Optional.of(asistente));
        when(inscripciones.bloquearPorCodigo(10L)).thenReturn(Optional.of(inscripcion));
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
        verify(inscripciones, never()).bloquearPorCodigo(any());
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
        pagoCompleto();
        Actividad actividad = mock(Actividad.class);
        when(actividad.getCodigo()).thenReturn(30L);
        when(actividad.getEstado()).thenReturn(EstadoActividad.ACTIVA);
        when(actividad.hayCupoDisponible()).thenReturn(true);
        when(actividades.bloquearCodigo(30L)).thenReturn(Optional.of(30L));
        when(actividades.findById(30L)).thenReturn(Optional.of(actividad));

        servicio.inscribirActividad("propia@example.invalid", 10L, 30L);

        assertEquals(1, inscripcion.getActividades().size());
        verify(inscripciones).saveAndFlush(inscripcion);
        verify(actividad).incrementarInscritos();
    }

    @Test
    void actividadCanceladaNoSeAgrega() {
        inscripcion.confirmar();
        pagoCompleto();
        Actividad actividad = mock(Actividad.class);
        when(actividades.bloquearCodigo(30L)).thenReturn(Optional.of(30L));
        when(actividad.getEstado()).thenReturn(EstadoActividad.CANCELADA);
        when(actividades.findById(30L)).thenReturn(Optional.of(actividad));

        assertThrows(IllegalStateException.class,
                () -> servicio.inscribirActividad("propia@example.invalid", 10L, 30L));
        verify(inscripciones, never()).saveAndFlush(any());
    }

    @Test
    void confirmadaSinPagoNoPuedeElegirActividad() {
        inscripcion.confirmar();
        assertThrows(IllegalStateException.class,
                () -> servicio.inscribirActividad("propia@example.invalid", 10L, 30L));
        verifyNoInteractions(actividades);
        verify(inscripciones, never()).saveAndFlush(any());
    }

    @Test
    void reenvioDeActividadNoConsumeCupoDosVeces() {
        inscripcion.confirmar();
        pagoCompleto();
        Actividad actividad = mock(Actividad.class);
        when(actividad.getCodigo()).thenReturn(30L);
        when(actividad.hayCupoDisponible()).thenReturn(true);
        when(actividad.getEstado()).thenReturn(EstadoActividad.ACTIVA);
        when(actividades.bloquearCodigo(30L)).thenReturn(Optional.of(30L));
        when(actividades.findById(30L)).thenReturn(Optional.of(actividad));

        servicio.inscribirActividad("propia@example.invalid", 10L, 30L);
        assertThrows(IllegalStateException.class,
                () -> servicio.inscribirActividad("propia@example.invalid", 10L, 30L));
        verify(actividad, times(1)).incrementarInscritos();
        verify(inscripciones, times(1)).saveAndFlush(inscripcion);
    }

    private void pagoCompleto() {
        Pago pago = new Pago(asistente, 50, ModalidadPago.COMPLETO, 1);
        pago.setEstado(EstadoPago.PAGADO);
        pago.setSaldo(0);
        pago.setInscripcion(inscripcion);
        when(pagos.findByInscripcion(inscripcion)).thenReturn(Optional.of(pago));
    }
}
