package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.RecuperacionServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.when;

@WebMvcTest(RecuperacionController.class)
@Import(ConfiguracionSeguridad.class)
class RecuperacionVistaTest {
    @Autowired private MockMvc mvc;
    @MockBean private RecuperacionServicio recuperacionServicio;
    @MockBean private UsuarioDAO usuarioDAO;

    @Test
    void formularioDeSolicitudEsPublico() throws Exception {
        mvc.perform(get("/recuperar"))
                .andExpect(status().isOk())
                .andExpect(view().name("recuperar"));
    }

    @Test
    void csrfEsObligatorioEnSolicitud() throws Exception {
        mvc.perform(post("/recuperar").param("correo", "ana@example.invalid"))
                .andExpect(status().isForbidden());
    }

    @Test
    void correoRegistradoYNoRegistradoRecibenLaMismaRespuestaGenerica() throws Exception {
        String mensajeEsperado =
                "Si el correo está registrado, recibirás instrucciones para restablecer la contraseña.";
        mvc.perform(post("/recuperar").with(csrf()).param("correo", "ana@example.invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recuperar"))
                .andExpect(flash().attribute("mensaje", mensajeEsperado));
        mvc.perform(post("/recuperar").with(csrf()).param("correo", "nadie@example.invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recuperar"))
                .andExpect(flash().attribute("mensaje", mensajeEsperado));
        verify(recuperacionServicio).solicitar("ana@example.invalid");
        verify(recuperacionServicio).solicitar("nadie@example.invalid");
    }

    @Test
    @WithMockUser(roles = "ASISTENTE")
    void asistentePuedeUsarElFormularioConCsrf() throws Exception {
        mvc.perform(post("/recuperar").with(csrf()).param("correo", "ana@example.invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recuperar"));
    }

    @Test
    void formularioDeTokenEsPublicoYPostSinCsrfSeRechaza() throws Exception {
        String token = "b02d8f1d-255c-4c5f-9f4d-c9328a871466";
        mvc.perform(get("/recuperar/" + token))
                .andExpect(status().isOk())
                .andExpect(view().name("restablecer-contrasena"));
        mvc.perform(post("/recuperar/" + token).param("contrasena", "nuevaClave123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void postConTokenValidoRespetaCsrfYPermiteCambiarClaveDesdeCualquierRol() throws Exception {
        String valorToken = "b02d8f1d-255c-4c5f-9f4d-c9328a871466";
        when(recuperacionServicio.restablecer(
                java.util.UUID.fromString(valorToken), "nuevaClave123")).thenReturn(true);

        mvc.perform(post("/recuperar/" + valorToken).with(csrf())
                        .param("contrasena", "nuevaClave123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/login"))
                .andExpect(flash().attributeExists("mensaje"));

        verify(recuperacionServicio).restablecer(
                java.util.UUID.fromString(valorToken), "nuevaClave123");
    }
}
