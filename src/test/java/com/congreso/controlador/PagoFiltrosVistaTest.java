package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.AsistenciaServicio;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.servicio.PagoServicio;
import com.congreso.servicio.UsuarioServicio;
import com.congreso.seguridad.ConfiguracionSeguridad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CongresoController.class, PagoServlet.class})
@Import(ConfiguracionSeguridad.class)
@WithMockUser(username = "admin@example.invalid", roles = "ADMINISTRADOR")
class PagoFiltrosVistaTest {

    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarioDAO;
    @MockBean private ActividadServicio actividadServicio;
    @MockBean private ColaboradorServicio colaboradorServicio;
    @MockBean private InscripcionServicio inscripcionServicio;
    @MockBean private AsistenciaServicio asistenciaServicio;
    @MockBean private PagoServicio pagoServicio;
    @MockBean private UsuarioServicio usuarioServicio;

    @Test
    void panelEnviaEstadoYBusquedaAlListadoReal() throws Exception {
        prepararDashboard();
        when(pagoServicio.filtrarPagosAdmin("PAGADO", "ana@example.invalid")).thenReturn(List.of());
        MockHttpSession session = sesionAdmin();

        mvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/pagos/admin/listar\"")))
                .andExpect(content().string(containsString("name=\"estado\"")))
                .andExpect(content().string(containsString("name=\"busqueda\"")))
                .andExpect(content().string(containsString("Limpiar filtros")));

        mvc.perform(get("/pagos/admin/listar").session(session)
                        .param("estado", "PAGADO")
                        .param("busqueda", "ana@example.invalid"))
                .andExpect(status().isOk());

        verify(pagoServicio).filtrarPagosAdmin("PAGADO", "ana@example.invalid");
    }

    @Test
    void estadoInvalidoMuestraErrorYConservaLosValores() throws Exception {
        when(pagoServicio.filtrarPagosAdmin("DESCONOCIDO", "ana"))
                .thenThrow(new IllegalArgumentException("Estado de pago no válido: DESCONOCIDO"));

        mvc.perform(get("/pagos/admin/listar").session(sesionAdmin())
                        .param("estado", "DESCONOCIDO")
                        .param("busqueda", "ana"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Estado de pago no válido")))
                .andExpect(content().string(containsString("value=\"DESCONOCIDO\"")))
                .andExpect(content().string(containsString("value=\"ana\"")));
    }

    private void prepararDashboard() {
        when(usuarioServicio.contarAsistentesActivos()).thenReturn(0L);
        when(usuarioServicio.contarAsistentesRegistradosHoy()).thenReturn(0L);
        when(pagoServicio.ingresosCubiertos()).thenReturn(0.0);
        when(pagoServicio.contarPagosPendientes()).thenReturn(0L);
        when(pagoServicio.obtenerPagosRecientes(5)).thenReturn(List.of());
        when(actividadServicio.obtenerActividadesActivas()).thenReturn(List.of());
        when(inscripcionServicio.contarInscripcionesPorEstadoReal()).thenReturn(Map.of(
                EstadoInscripcion.PENDIENTE, 0L,
                EstadoInscripcion.CONFIRMADA, 0L,
                EstadoInscripcion.CANCELADA, 0L));
    }

    private MockHttpSession sesionAdmin() {
        MockHttpSession session = new MockHttpSession();
        Administrador admin = new Administrador();
        admin.setRol(RolUsuario.ADMINISTRADOR);
        session.setAttribute("usuario", admin);
        return session;
    }
}
