package com.congreso.servicio;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoPago;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoFiltrosServicioTest {

    @Mock private PagoDAO pagoDAO;
    @Mock private AsistenteDAO asistenteDAO;
    @Mock private InscripcionDAO inscripcionDAO;
    @InjectMocks private PagoServicio servicio;

    private Pago pago(String nombre, String apellido, String correo, EstadoPago estado) {
        Asistente asistente = new Asistente();
        asistente.setNombres(nombre);
        asistente.setApellidos(apellido);
        asistente.setCorreo(correo);
        Pago pago = new Pago();
        pago.setAsistente(asistente);
        pago.setEstado(estado);
        return pago;
    }

    @Test
    void sinFiltrosDevuelveTodos() {
        Pago ana = pago("Ana", "Quispe", "ana@example.test", EstadoPago.PAGADO);
        when(pagoDAO.findAll()).thenReturn(List.of(ana));
        assertEquals(List.of(ana), servicio.filtrarPagos(null, ""));
    }

    @Test
    void filtroCombinadoBuscaPorNombreSinDistinguirMayusculas() {
        Pago ana = pago("Ana", "Quispe", "ana@example.test", EstadoPago.PAGADO);
        Pago maria = pago("Maria", "Torres", "maria@example.test", EstadoPago.PAGADO);
        when(pagoDAO.findByEstadoIn(List.of(EstadoPago.PAGADO))).thenReturn(List.of(ana, maria));
        assertEquals(List.of(ana), servicio.filtrarPagos("pagado", "  ANA quIsPe  "));
    }

    @Test
    void tambienPermiteBuscarPorCorreo() {
        Pago ana = pago("Ana", "Quispe", "ana@example.test", EstadoPago.PAGADO);
        when(pagoDAO.findAll()).thenReturn(List.of(ana));
        assertEquals(List.of(ana), servicio.filtrarPagos("", "EXAMPLE.TEST"));
    }

    @Test
    void sinCoincidenciasDevuelveListaVacia() {
        when(pagoDAO.findAll()).thenReturn(List.of(
                pago("Ana", "Quispe", "ana@example.test", EstadoPago.PAGADO)));
        assertTrue(servicio.filtrarPagos(null, "nombre inexistente").isEmpty());
    }

    @Test
    void estadoInvalidoSeRechazaEnLugarDeOcultarError() {
        assertThrows(IllegalArgumentException.class, () -> servicio.filtrarPagos("inventado", ""));
        verifyNoInteractions(pagoDAO);
    }
}
