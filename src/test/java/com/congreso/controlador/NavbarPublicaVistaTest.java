package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.AsistenciaServicio;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.servicio.PagoServicio;
import com.congreso.servicio.UsuarioServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CongresoController.class)
@Import(ConfiguracionSeguridad.class)
class NavbarPublicaVistaTest {
    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarioDAO;
    @MockBean private ActividadServicio actividadServicio;
    @MockBean private ColaboradorServicio colaboradorServicio;
    @MockBean private InscripcionServicio inscripcionServicio;
    @MockBean private AsistenciaServicio asistenciaServicio;
    @MockBean private PagoServicio pagoServicio;
    @MockBean private UsuarioServicio usuarioServicio;

    @Test
    void portadaCargaBootstrapLocalAntesDelEstiloPropio() throws Exception {
        when(actividadServicio.obtenerActividadesActivas()).thenReturn(List.of());
        when(colaboradorServicio.listarPonentes()).thenReturn(List.of());
        String html = mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/bootstrap.min.css?v=5.3.0")))
                .andExpect(content().string(containsString("/js/bootstrap.bundle.min.js?v=5.3.0")))
                .andExpect(content().string(not(containsString("cdn.jsdelivr.net/npm/bootstrap"))))
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("navbar-expand-lg"));
        assertTrue(html.indexOf("</nav>") < html.indexOf("id=\"menuPublico\""),
                "El panel móvil debe estar fuera de la barra de navegación de escritorio");
    }

    @Test
    void bootstrapSeSirveDesdeLaAplicacion() throws Exception {
        mvc.perform(get("/css/bootstrap.min.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".offcanvas")));
        mvc.perform(get("/js/bootstrap.bundle.min.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Offcanvas")));
    }
}
