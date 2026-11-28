package com.congreso.controlador;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.servicio.NotificacionServicio;
import com.congreso.servicio.PagoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InscripcionServlet.class)
@Import(ConfiguracionSeguridad.class)
class InscripcionConfirmacionVistaTest {
    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarios;
    @MockBean private AsistenteDAO asistentes;
    @MockBean private InscripcionServicio inscripciones;
    @MockBean private PagoServicio pagos;
    @MockBean private NotificacionServicio notificaciones;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void confirmacionPostSinCsrfSeRechaza() throws Exception {
        mvc.perform(post("/inscripciones/confirmar/17"))
                .andExpect(status().isForbidden());

        verify(inscripciones, never()).confirmarInscripcion(17L);
    }

    @Test
    @WithMockUser(roles = "ASISTENTE")
    void confirmacionAdminEsDenegadaAlAsistente() throws Exception {
        mvc.perform(post("/inscripciones/confirmar/17").with(csrf()))
                .andExpect(status().isForbidden());

        verify(inscripciones, never()).confirmarInscripcion(17L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void pagoIncompletoDevuelveElMotivoSinAnunciarConfirmacion() throws Exception {
        String motivo = "No se puede confirmar la inscripción: el pago simulado debe estar completo "
                + "(estado PAGADO y saldo S/ 0.00).";
        doThrow(new IllegalStateException(motivo))
                .when(inscripciones).confirmarInscripcion(17L);

        MvcResult resultado = mvc.perform(post("/inscripciones/confirmar/17").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inscripciones/admin/listar"))
                .andExpect(flash().attribute("error", motivo))
                .andReturn();

        when(inscripciones.listarTodasLasInscripciones()).thenReturn(List.of());
        MockHttpSession session = (MockHttpSession) resultado.getRequest().getSession();
        mvc.perform(get("/inscripciones/admin/listar").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(motivo)));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void confirmacionCompletaMuestraExitoSoloTrasElServicio() throws Exception {
        when(inscripciones.confirmarInscripcion(17L)).thenReturn(true);

        mvc.perform(post("/inscripciones/confirmar/17").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inscripciones/admin/listar"))
                .andExpect(flash().attribute("mensaje", "Inscripción confirmada"));

        verify(inscripciones).confirmarInscripcion(17L);
    }
}
