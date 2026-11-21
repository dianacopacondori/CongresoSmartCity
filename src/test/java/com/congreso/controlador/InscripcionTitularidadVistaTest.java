package com.congreso.controlador;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Asistente;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.servicio.PagoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InscripcionServlet.class)
@Import(ConfiguracionSeguridad.class)
class InscripcionTitularidadVistaTest {
    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarios;
    @MockBean private AsistenteDAO asistentes;
    @MockBean private InscripcionServicio inscripciones;
    @MockBean private PagoServicio pagos;

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void usaLaIdentidadAutenticadaAunqueSesionContengaOtraCuenta() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        Asistente otra = new Asistente();
        otra.setCodigo(99L);
        sesion.setAttribute("usuario", otra);
        mvc.perform(post("/inscripciones/agregar-actividad").session(sesion).with(csrf())
                        .param("codigoInscripcion", "10").param("codigoActividad", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inscripciones/mi-agenda"))
                .andExpect(flash().attributeExists("mensaje"));
        verify(inscripciones).inscribirActividad("ana@example.invalid", 10L, 30L);
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void inscripcionAjenaDevuelve403SinExito() throws Exception {
        when(inscripciones.inscribirActividad("ana@example.invalid", 10L, 30L))
                .thenThrow(new AccessDeniedException("Inscripción ajena"));
        mvc.perform(post("/inscripciones/agregar-actividad").with(csrf())
                        .param("codigoInscripcion", "10").param("codigoActividad", "30"))
                .andExpect(status().isForbidden())
                .andExpect(flash().attributeCount(0));
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void inscripcionPendienteNoMuestraExito() throws Exception {
        when(inscripciones.inscribirActividad("ana@example.invalid", 10L, 30L))
                .thenThrow(new IllegalStateException("Inscripción no confirmada"));
        mvc.perform(post("/inscripciones/agregar-actividad").with(csrf())
                        .param("codigoInscripcion", "10").param("codigoActividad", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorNoPuedeUsarRutaPersonal() throws Exception {
        mvc.perform(post("/inscripciones/agregar-actividad").with(csrf())
                        .param("codigoInscripcion", "10").param("codigoActividad", "30"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(inscripciones);
    }

    @Test
    @WithMockUser(roles = "ASISTENTE")
    void postSinCsrfEsRechazado() throws Exception {
        mvc.perform(post("/inscripciones/agregar-actividad")
                        .param("codigoInscripcion", "10").param("codigoActividad", "30"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(inscripciones);
    }
}
