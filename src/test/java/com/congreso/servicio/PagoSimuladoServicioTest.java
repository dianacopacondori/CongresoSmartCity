package com.congreso.servicio;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.modelo.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PagoSimuladoServicioTest {
    private PagoDAO pagos;
    private AsistenteDAO asistentes;
    private InscripcionDAO inscripciones;
    private PagoServicio servicio;
    private Asistente asistente;
    private Inscripcion inscripcion;

    @BeforeEach
    void setUp() {
        pagos = mock(PagoDAO.class);
        asistentes = mock(AsistenteDAO.class);
        inscripciones = mock(InscripcionDAO.class);
        servicio = new PagoServicio(pagos, asistentes, inscripciones);
        asistente = new Asistente();
        asistente.setCodigo(1L);
        asistente.setCorreo("propia@example.invalid");
        asistente.setRol(RolUsuario.ASISTENTE);
        inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.setCodigo(10L);
        when(asistentes.findByCorreo(asistente.getCorreo())).thenReturn(Optional.of(asistente));
        when(inscripciones.bloquearPorCodigo(10L)).thenReturn(Optional.of(inscripcion));
        when(pagos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void estudiantePagaPrimeraCuotaSinConfirmacion() {
        Pago pago = servicio.simularPago(asistente.getCorreo(), 10L, ModalidadPago.CUOTAS);
        assertEquals(50, pago.getImporte());
        assertEquals(25, pago.getSaldo());
        assertEquals(2, pago.getCuotas());
        assertEquals(EstadoPago.PARCIAL, pago.getEstado());
        assertEquals(EstadoInscripcion.PENDIENTE, inscripcion.getEstado());
        assertSame(inscripcion, pago.getInscripcion());
        assertEquals(1, pago.getHistorialCuotas().size());
        assertEquals(25, pago.getHistorialCuotas().get(0).getImporte());
        assertEquals(1, pago.getHistorialCuotas().get(0).getNumeroCuota());
        verify(pagos).saveAndFlush(pago);
    }

    @Test
    void estudiantePagaCompletoYSeConfirma() {
        Pago pago = servicio.simularPago(asistente.getCorreo(), 10L, ModalidadPago.COMPLETO);
        assertEquals(50, pago.getImporte());
        assertEquals(0, pago.getSaldo());
        assertEquals(EstadoPago.PAGADO, pago.getEstado());
        assertTrue(inscripcion.isConfirmacion());
        assertEquals(1, pago.getHistorialCuotas().size());
        assertEquals(50, pago.getHistorialCuotas().get(0).getImporte());
        verify(inscripciones).saveAndFlush(inscripcion);
    }

    @Test
    void externoNoTieneCuotasNiPagoDuplicado() {
        inscripcion.setTipo("Externo");
        assertThrows(IllegalArgumentException.class,
                () -> servicio.simularPago(asistente.getCorreo(), 10L, ModalidadPago.CUOTAS));
        verify(pagos, never()).saveAndFlush(any());
        Pago completo = servicio.simularPago(asistente.getCorreo(), 10L, ModalidadPago.COMPLETO);
        assertEquals(100, completo.getImporte());
        when(pagos.findByInscripcion(inscripcion)).thenReturn(Optional.of(completo));
        inscripcion.setEstado(EstadoInscripcion.PENDIENTE);
        inscripcion.setConfirmacion(false);
        assertThrows(IllegalStateException.class,
                () -> servicio.simularPago(asistente.getCorreo(), 10L, ModalidadPago.COMPLETO));
        verify(pagos, times(1)).saveAndFlush(any());
    }

    @Test
    void noPermitePagarInscripcionAjena() {
        Asistente otra = new Asistente();
        otra.setCodigo(2L);
        inscripcion.setAsistente(otra);
        assertThrows(AccessDeniedException.class,
                () -> servicio.simularPago(asistente.getCorreo(), 10L, ModalidadPago.COMPLETO));
        verify(pagos, never()).saveAndFlush(any());
    }

    @Test
    void segundaCuotaSoloUnaVezYPorSuTitular() {
        Pago pago = new Pago(asistente, 50, ModalidadPago.CUOTAS, 2);
        pago.setCodigoPago(11L);
        pago.setInscripcion(inscripcion);
        pago.registrarPago(25);
        when(pagos.findById(11L)).thenReturn(Optional.of(pago));
        when(pagos.findByInscripcion(inscripcion)).thenReturn(Optional.of(pago));

        servicio.simularSegundaCuota(asistente.getCorreo(), 11L);
        assertEquals(0, pago.getSaldo());
        assertEquals(EstadoPago.PAGADO, pago.getEstado());
        assertEquals(EstadoInscripcion.CONFIRMADA, inscripcion.getEstado());
        assertEquals(2, pago.getHistorialCuotas().size());
        assertEquals(25, pago.getHistorialCuotas().get(1).getImporte());
        assertEquals(2, pago.getHistorialCuotas().get(1).getNumeroCuota());
        assertThrows(IllegalStateException.class, () -> servicio.simularSegundaCuota(asistente.getCorreo(), 11L));
        verify(pagos, times(1)).saveAndFlush(pago);
    }

    @Test
    void noPermiteSegundaCuotaDeUnPagoAjeno() {
        Asistente otra = new Asistente();
        otra.setCodigo(2L);
        Inscripcion ajena = new Inscripcion(otra, "Estudiante");
        ajena.setCodigo(20L);
        Pago pago = new Pago(otra, 50, ModalidadPago.CUOTAS, 2);
        pago.setInscripcion(ajena);
        pago.setCodigoPago(22L);
        pago.registrarPago(25);
        when(pagos.findById(22L)).thenReturn(Optional.of(pago));
        when(inscripciones.bloquearPorCodigo(20L)).thenReturn(Optional.of(ajena));
        assertThrows(AccessDeniedException.class,
                () -> servicio.simularSegundaCuota(asistente.getCorreo(), 22L));
        verify(pagos, never()).saveAndFlush(any());
    }

    @Test
    void rechazaTipoDesconocidoYModalidadAusente() {
        inscripcion.setTipo("Otro");
        assertThrows(IllegalArgumentException.class,
                () -> servicio.simularPago(asistente.getCorreo(), 10L, ModalidadPago.COMPLETO));
        inscripcion.setTipo("Estudiante");
        assertThrows(IllegalArgumentException.class,
                () -> servicio.simularPago(asistente.getCorreo(), 10L, null));
        verify(pagos, never()).saveAndFlush(any());
    }
}
