package com.congreso.controlador;

import com.congreso.modelo.Administrador;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.ReporteServicio;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Un error interno no debe filtrar detalles de la base de datos ni de la consulta.
 */
class ReporteServletSeguridadTest {

    @Test
    void elMensajeDeErrorNoRevelaDetallesInternos() {
        ReporteServicio servicio = mock(ReporteServicio.class);
        when(servicio.generarReporte(eq("ASISTENTES"), isNull(), isNull(), isNull()))
                .thenThrow(new IllegalStateException("jdbc:postgresql://localhost:5433/congreso password=postgres"));
        ReporteServlet controlador = new ReporteServlet(servicio);

        MockHttpSession sesion = new MockHttpSession();
        Administrador administrador = new Administrador();
        administrador.setRol(RolUsuario.ADMINISTRADOR);
        sesion.setAttribute("usuario", administrador);

        ExtendedModelMap modelo = new ExtendedModelMap();
        String vista = controlador.generarReporte("ASISTENTES", null, null, null, sesion, modelo);

        assertEquals("admin/reportes-menu", vista);
        String error = modelo.get("error").toString();
        assertEquals("No se pudo generar el reporte. Intente nuevamente.", error);
        assertFalse(error.contains("postgres"));
        assertFalse(error.contains("jdbc:"));
    }

    @Test
    void unUsuarioSinRolAdministradorNoPuedeGenerarReportes() {
        ReporteServicio servicio = mock(ReporteServicio.class);
        ReporteServlet controlador = new ReporteServlet(servicio);

        assertEquals("redirect:/usuarios/login",
                controlador.generarReporte("ASISTENTES", null, null, null, new MockHttpSession(), new ExtendedModelMap()));
        org.mockito.Mockito.verifyNoInteractions(servicio);
    }
}
