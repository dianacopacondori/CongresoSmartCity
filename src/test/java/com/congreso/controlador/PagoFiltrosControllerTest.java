package com.congreso.controlador;

import com.congreso.modelo.Pago;
import com.congreso.servicio.PagoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PagoFiltrosControllerTest {
    private final PagoServicio servicio = mock(PagoServicio.class);
    private final PagoServlet controller = new PagoServlet(servicio);

    @Test
    void enviaLosFiltrosAlServicioYLosConservaEnLaVista() {
        Pago pago = new Pago();
        when(servicio.filtrarPagos("PAGADO", "Ana")).thenReturn(List.of(pago));
        Model model = new ExtendedModelMap();

        assertEquals("admin/pagos-lista", controller.listarPagos("PAGADO", "Ana", model));
        assertEquals(List.of(pago), model.getAttribute("pagos"));
        assertEquals("PAGADO", model.getAttribute("estado"));
        assertEquals("Ana", model.getAttribute("nombre"));
        verify(servicio).filtrarPagos("PAGADO", "Ana");
    }

    @Test
    void estadoInvalidoMuestraError() {
        when(servicio.filtrarPagos("OTRO", null))
                .thenThrow(new IllegalArgumentException("Estado invalido"));
        Model model = new ExtendedModelMap();
        assertEquals("admin/pagos-lista", controller.listarPagos("OTRO", null, model));
        assertEquals("Estado invalido", model.getAttribute("error"));
        assertEquals(List.of(), model.getAttribute("pagos"));
    }
}
