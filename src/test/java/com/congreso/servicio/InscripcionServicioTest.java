package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.RolUsuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscripcionServicioTest {
    @Mock private InscripcionDAO inscripcionDAO;
    @Mock private AsistenteDAO asistenteDAO;
    @Mock private ActividadDAO actividadDAO;
    @Mock private PagoDAO pagoDAO;
    @InjectMocks private InscripcionServicio servicio;

    private Asistente asistente() {
        Asistente asistente = new Asistente();
        asistente.setCodigo(10L);
        asistente.setRol(RolUsuario.ASISTENTE);
        return asistente;
    }

    @Test
    void permiteCrearInscripcionEstudianteValida() {
        Asistente asistente = asistente();
        when(inscripcionDAO.findByAsistente(asistente)).thenReturn(List.of());
        when(inscripcionDAO.save(any())).thenAnswer(i -> i.getArgument(0));

        Inscripcion creada = servicio.crearInscripcion(asistente, "Estudiante");
        assertEquals("Estudiante", creada.getTipo());
        assertEquals(EstadoInscripcion.PENDIENTE, creada.getEstado());
    }

    @Test
    void rechazaTipoDeInscripcionArbitrario() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearInscripcion(asistente(), "InvitadoGratis"));
        verify(inscripcionDAO, never()).save(any());
    }

    @Test
    void rechazaInscripcionesDuplicadasNoCanceladas() {
        Asistente asistente = asistente();
        when(inscripcionDAO.findByAsistente(asistente))
                .thenReturn(List.of(new Inscripcion(asistente, "Estudiante")));
        assertThrows(IllegalStateException.class,
                () -> servicio.crearInscripcion(asistente, "Externo"));
        verify(inscripcionDAO, never()).save(any());
    }

    @Test
    void rechazaConfirmarSinPagoCompleto() {
        Inscripcion inscripcion = new Inscripcion(asistente(), "Estudiante");
        when(inscripcionDAO.findById(3L)).thenReturn(Optional.of(inscripcion));
        when(pagoDAO.findByInscripcion(inscripcion)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> servicio.confirmarInscripcion(3L));
        verify(inscripcionDAO, never()).save(any());
    }

    @Test
    void confirmaSoloCuandoElPagoEstaCompletado() {
        Asistente asistente = asistente();
        Inscripcion inscripcion = new Inscripcion(asistente, "Estudiante");
        Pago pago = new Pago();
        pago.setAsistente(asistente);
        pago.setEstado(EstadoPago.PAGADO);
        pago.setSaldo(0);
        when(inscripcionDAO.findById(3L)).thenReturn(Optional.of(inscripcion));
        when(pagoDAO.findByInscripcion(inscripcion)).thenReturn(Optional.of(pago));

        assertTrue(servicio.confirmarInscripcion(3L));
        assertEquals(EstadoInscripcion.CONFIRMADA, inscripcion.getEstado());
        assertTrue(inscripcion.isConfirmacion());
        verify(inscripcionDAO).save(inscripcion);
    }

    @Test
    void rechazaConfirmarInscripcionCancelada() {
        Inscripcion inscripcion = new Inscripcion(asistente(), "Estudiante");
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        when(inscripcionDAO.findById(3L)).thenReturn(Optional.of(inscripcion));
        assertThrows(IllegalStateException.class, () -> servicio.confirmarInscripcion(3L));
    }
}
