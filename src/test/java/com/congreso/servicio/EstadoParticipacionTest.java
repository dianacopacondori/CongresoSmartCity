package com.congreso.servicio;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.dto.EstadoParticipacion;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.modelo.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EstadoParticipacionTest {
    private Asistente asistente;
    private InscripcionDAO inscripciones;
    private PagoDAO pagos;
    private PagoServicio servicio;

    @BeforeEach
    void configurar() {
        asistente = new Asistente();
        asistente.setCodigo(1L);
        asistente.setCorreo("demo@example.invalid");
        asistente.setRol(RolUsuario.ASISTENTE);
        AsistenteDAO asistentes = mock(AsistenteDAO.class);
        inscripciones = mock(InscripcionDAO.class);
        pagos = mock(PagoDAO.class);
        when(asistentes.findByCorreo(asistente.getCorreo())).thenReturn(Optional.of(asistente));
        servicio = new PagoServicio(pagos, asistentes, inscripciones);
    }

    @Test
    void sinInscripcionOfreceCrearInscripcion() {
        when(inscripciones.findByAsistente(asistente)).thenReturn(List.of());
        assertEquals(EstadoParticipacion.Paso.CREAR_INSCRIPCION,
                servicio.estadoParticipacion(asistente.getCorreo()).paso());
    }

    @Test
    void pendienteSinPagoOfreceSimularPago() {
        Inscripcion pendiente = inscripcion(4L, EstadoInscripcion.PENDIENTE);
        when(inscripciones.findByAsistente(asistente)).thenReturn(List.of(pendiente));
        when(pagos.findByAsistente(asistente)).thenReturn(List.of());
        var estado = servicio.estadoParticipacion(asistente.getCorreo());
        assertEquals(EstadoParticipacion.Paso.SIMULAR_PAGO, estado.paso());
        assertEquals("SIN PAGOS", estado.pago());
        assertEquals(4L, estado.codigoInscripcion());
    }

    @Test
    void primeraCuotaOfreceContinuarYNoAnunciaConfirmacion() {
        Inscripcion pendiente = inscripcion(4L, EstadoInscripcion.PENDIENTE);
        Pago parcial = pago(1L, pendiente, EstadoPago.PARCIAL, 25);
        when(inscripciones.findByAsistente(asistente)).thenReturn(List.of(pendiente));
        when(pagos.findByAsistente(asistente)).thenReturn(List.of(parcial));
        var estado = servicio.estadoParticipacion(asistente.getCorreo());
        assertEquals(EstadoParticipacion.Paso.COMPLETAR_CUOTA, estado.paso());
        assertEquals("PARCIAL", estado.pago());
        assertEquals("PENDIENTE", estado.inscripcion());
    }

    @Test
    void noMezclaPagoAntiguoConNuevaInscripcion() {
        Inscripcion confirmada = inscripcion(3L, EstadoInscripcion.CONFIRMADA);
        Inscripcion pendiente = inscripcion(4L, EstadoInscripcion.PENDIENTE);
        Pago anterior = pago(1L, confirmada, EstadoPago.PAGADO, 0);
        when(inscripciones.findByAsistente(asistente)).thenReturn(List.of(pendiente, confirmada));
        when(pagos.findByAsistente(asistente)).thenReturn(List.of(anterior));
        var estado = servicio.estadoParticipacion(asistente.getCorreo());
        assertEquals(4L, estado.codigoInscripcion());
        assertEquals(EstadoParticipacion.Paso.SIMULAR_PAGO, estado.paso());
        assertEquals("SIN PAGOS", estado.pago());
    }

    @Test
    void confirmadaYPagadaOfreceAgendaPeroConfirmadaSinPagoPideRevision() {
        Inscripcion confirmada = inscripcion(3L, EstadoInscripcion.CONFIRMADA);
        when(inscripciones.findByAsistente(asistente)).thenReturn(List.of(confirmada));
        when(pagos.findByAsistente(asistente)).thenReturn(List.of(pago(1L, confirmada, EstadoPago.PAGADO, 0)));
        assertEquals(EstadoParticipacion.Paso.VER_AGENDA, servicio.estadoParticipacion(asistente.getCorreo()).paso());
        when(pagos.findByAsistente(asistente)).thenReturn(List.of());
        assertEquals(EstadoParticipacion.Paso.REVISAR_ESTADO, servicio.estadoParticipacion(asistente.getCorreo()).paso());
    }

    private Inscripcion inscripcion(Long codigo, EstadoInscripcion estado) {
        Inscripcion inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.setCodigo(codigo);
        inscripcion.setEstado(estado);
        return inscripcion;
    }

    private Pago pago(Long codigo, Inscripcion inscripcion, EstadoPago estado, double saldo) {
        Pago pago = new Pago(asistente, 50, ModalidadPago.CUOTAS, 2);
        pago.setCodigoPago(codigo);
        pago.setInscripcion(inscripcion);
        pago.setEstado(estado);
        pago.setSaldo(saldo);
        return pago;
    }
}
