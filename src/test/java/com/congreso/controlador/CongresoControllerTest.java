package com.congreso.controlador;

import com.congreso.modelo.Administrador;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.AsistenciaServicio;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.servicio.PagoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class CongresoControllerTest {

    private final CongresoController controller = new CongresoController(
            mock(ActividadServicio.class), mock(ColaboradorServicio.class),
            mock(InscripcionServicio.class), mock(AsistenciaServicio.class), mock(PagoServicio.class));

    @Test
    void administradorNoSeConvierteEnAsistente() {
        MockHttpSession session = new MockHttpSession();
        Administrador administrador = new Administrador();
        administrador.setRol(RolUsuario.ADMINISTRADOR);
        session.setAttribute("usuario", administrador);

        assertEquals("redirect:/admin/dashboard", controller.dashboardAsistente(new ExtendedModelMap(), session));
    }

    @Test
    void sesionConTipoYRolIncompatiblesSeRechazaSinError500() {
        MockHttpSession session = new MockHttpSession();
        Administrador administrador = new Administrador();
        administrador.setRol(RolUsuario.ASISTENTE);
        session.setAttribute("usuario", administrador);

        assertEquals("redirect:/usuarios/login", controller.dashboardAsistente(new ExtendedModelMap(), session));
    }
}
