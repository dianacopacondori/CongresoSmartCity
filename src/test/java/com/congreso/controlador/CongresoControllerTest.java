package com.congreso.controlador;

import com.congreso.modelo.Administrador;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.AsistenciaServicio;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Pago;
import com.congreso.servicio.PagoServicio;
import com.congreso.servicio.UsuarioServicio;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CongresoControllerTest {

    private final ActividadServicio actividadServicio = mock(ActividadServicio.class);
    private final PagoServicio pagoServicio = mock(PagoServicio.class);
    private final UsuarioServicio usuarioServicio = mock(UsuarioServicio.class);
    private final CongresoController controller = new CongresoController(
            actividadServicio, mock(ColaboradorServicio.class),
            mock(InscripcionServicio.class), mock(AsistenciaServicio.class),
            pagoServicio, usuarioServicio);

    @Test
    void administradorNoSeConvierteEnAsistente() {
        MockHttpSession session = new MockHttpSession();
        Administrador administrador = new Administrador();
        administrador.setRol(RolUsuario.ADMINISTRADOR);
        session.setAttribute("usuario", administrador);

        assertEquals("redirect:/admin/dashboard", controller.dashboardAsistente(new ExtendedModelMap(), session, null));
    }

    @Test
    void sesionConTipoYRolIncompatiblesSeRechazaSinError500() {
        MockHttpSession session = new MockHttpSession();
        Administrador administrador = new Administrador();
        administrador.setRol(RolUsuario.ASISTENTE);
        session.setAttribute("usuario", administrador);

        assertEquals("redirect:/usuarios/login", controller.dashboardAsistente(new ExtendedModelMap(), session, null));
    }

    @Test
    void dashboardAdminUsaCifrasRealesDeLosServicios() {
        when(usuarioServicio.contarAsistentesActivos()).thenReturn(7L);
        when(usuarioServicio.contarAsistentesRegistradosHoy()).thenReturn(2L);
        when(pagoServicio.ingresosCubiertos()).thenReturn(175.5);
        when(pagoServicio.contarPagosPendientes()).thenReturn(3L);
        when(actividadServicio.obtenerActividadesActivas()).thenReturn(List.of(mock(Actividad.class)));
        when(pagoServicio.obtenerPagosRecientes(5)).thenReturn(List.of(mock(Pago.class)));

        Model model = new ExtendedModelMap();
        assertEquals("dashboard-admin", controller.dashboardAdmin(model, new MockHttpSession()));

        assertEquals(7L, model.getAttribute("totalAsistentes"));
        assertEquals(2L, model.getAttribute("registradosHoy"));
        assertEquals("175.50", model.getAttribute("totalIngresos"));
        assertEquals(3L, model.getAttribute("pagosPendientes"));
        assertEquals(1, ((List<?>) model.getAttribute("actividades")).size());
        assertEquals(1, ((List<?>) model.getAttribute("pagos")).size());
    }

    @Test
    void dashboardAdminSinDatosMuestraCeroYListasVacias() {
        when(usuarioServicio.contarAsistentesActivos()).thenReturn(0L);
        when(usuarioServicio.contarAsistentesRegistradosHoy()).thenReturn(0L);
        when(pagoServicio.ingresosCubiertos()).thenReturn(0.0);
        when(pagoServicio.contarPagosPendientes()).thenReturn(0L);
        when(actividadServicio.obtenerActividadesActivas()).thenReturn(List.of());
        when(pagoServicio.obtenerPagosRecientes(5)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        controller.dashboardAdmin(model, new MockHttpSession());

        assertEquals(0L, model.getAttribute("totalAsistentes"));
        assertEquals("0.00", model.getAttribute("totalIngresos"));
        assertTrue(((List<?>) model.getAttribute("actividades")).isEmpty());
        assertTrue(((List<?>) model.getAttribute("pagos")).isEmpty());
    }
}
