package com.congreso.servicio;

import com.congreso.modelo.CuotaPago;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroCuotasTest {

    private Pago pago(double importe) {
        return new Pago(null, importe, ModalidadPago.CUOTAS, 2);
    }

    @Test
    void primeraCuotaDejaSaldoYRegistraHistorial() {
        Pago pago = pago(50);

        CuotaPago cuota = pago.registrarPago(25);

        assertEquals(25, pago.getSaldo());
        assertEquals(EstadoPago.PARCIAL, pago.getEstado());
        assertEquals(1, pago.getHistorialCuotas().size());
        assertEquals(1, cuota.getNumeroCuota());
        assertEquals(25, cuota.getImporte());
        assertEquals(LocalDate.now(), cuota.getFechaRegistro());
    }

    @Test
    void segundaCuotaCierraElPagoYNumeracionSigue() {
        Pago pago = pago(50);
        pago.registrarPago(25);
        CuotaPago segunda = pago.registrarPago(25);

        assertEquals(0, pago.getSaldo());
        assertEquals(EstadoPago.PAGADO, pago.getEstado());
        assertEquals(2, pago.getHistorialCuotas().size());
        assertEquals(2, segunda.getNumeroCuota());
    }

    @Test
    void pagoCompletoRegistraUnaSolaCuota() {
        Pago pago = pago(100);

        pago.registrarPago(100);

        assertEquals(0, pago.getSaldo());
        assertEquals(EstadoPago.PAGADO, pago.getEstado());
        assertEquals(1, pago.getHistorialCuotas().size());
        assertEquals(100, pago.getHistorialCuotas().get(0).getImporte());
    }

    @Test
    void rechazaImportesInvalidos() {
        Pago pago = pago(50);

        assertThrows(IllegalArgumentException.class, () -> pago.registrarPago(0));
        assertThrows(IllegalArgumentException.class, () -> pago.registrarPago(-5));
        assertThrows(IllegalArgumentException.class, () -> pago.registrarPago(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> pago.registrarPago(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> pago.registrarPago(50.01));
        assertTrue(pago.getHistorialCuotas().isEmpty());
        assertEquals(50, pago.getSaldo());
        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
    }

    @Test
    void rechazaAbonosSobrePagoPagadoOAnulado() {
        Pago pagado = pago(50);
        pagado.registrarPago(50);
        assertThrows(IllegalStateException.class, () -> pagado.registrarPago(1));

        Pago anulado = pago(50);
        anulado.setEstado(EstadoPago.ANULADO);
        assertThrows(IllegalStateException.class, () -> anulado.registrarPago(1));
    }

    @Test
    void noAceptaSobrepagoAunConToleranciaDecimal() {
        Pago pago = pago(50);
        pago.registrarPago(25);

        assertThrows(IllegalArgumentException.class, () -> pago.registrarPago(25.0001));
        assertEquals(25, pago.getSaldo());
        assertEquals(1, pago.getHistorialCuotas().size());
    }
}
