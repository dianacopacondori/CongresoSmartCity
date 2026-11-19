package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.dto.PerfilAsistenteFormulario;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.AsistenteServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsistenteServlet.class)
@Import(ConfiguracionSeguridad.class)
class AsistentePerfilVistaTest {
    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarioDAO;
    @MockBean private AsistenteServicio asistenteServicio;

    private Asistente asistente;

    @BeforeEach
    void setUp() {
        asistente = new Asistente();
        asistente.setCodigo(1L);
        asistente.setNombres("Ana");
        asistente.setApellidos("Torres");
        asistente.setIdentificacion("12345678");
        asistente.setCorreo("ana@example.com");
        asistente.setContrasena("hash-privado");
        asistente.setRol(RolUsuario.ASISTENTE);
    }

    @Test
    @WithMockUser(username = "ana@example.com", roles = "ASISTENTE")
    void muestraUnFormularioSinDatosDeAutorizacionEditables() throws Exception {
        when(asistenteServicio.buscarPerfilPropio("ana@example.com")).thenReturn(asistente);
        mvc.perform(get("/asistentes/mi-perfil"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/asistentes/mi-perfil\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("hash-privado"))))
                .andExpect(content().string(not(containsString("name=\"codigo\""))))
                .andExpect(content().string(not(containsString("name=\"rol\""))));
    }

    @Test
    @WithMockUser(username = "ana@example.com", roles = "ASISTENTE")
    void ignoraCamposManipuladosYUsaLaIdentidadAutenticada() throws Exception {
        when(asistenteServicio.actualizarPerfilPropio(eq("ana@example.com"), any())).thenReturn(asistente);
        MockHttpSession session = new MockHttpSession();
        Asistente otraPersona = new Asistente();
        otraPersona.setCodigo(99L);
        otraPersona.setCorreo("otro@example.com");
        session.setAttribute("usuario", otraPersona);
        mvc.perform(post("/asistentes/mi-perfil").with(csrf()).session(session)
                        .param("nombres", "Ana María").param("apellidos", "Torres")
                        .param("codigo", "99").param("identificacion", "00000000")
                        .param("correo", "otro@example.com").param("contrasena", "maliciosa")
                        .param("rol", "ADMINISTRADOR").param("estado", "ACTIVO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/asistentes/mi-perfil"))
                .andExpect(flash().attributeExists("mensaje"));
        verify(asistenteServicio).actualizarPerfilPropio(eq("ana@example.com"),
                org.mockito.ArgumentMatchers.argThat(perfil ->
                        perfil.getNombres().equals("Ana María") && perfil.getApellidos().equals("Torres")));
        assertEquals(asistente, session.getAttribute("usuario"));
    }

    @Test
    @WithMockUser(username = "ana@example.com", roles = "ASISTENTE")
    void falloAlGuardarNoAnunciaExito() throws Exception {
        when(asistenteServicio.actualizarPerfilPropio(eq("ana@example.com"), any()))
                .thenThrow(new DataIntegrityViolationException("error de base de datos"));
        when(asistenteServicio.buscarPerfilPropio("ana@example.com")).thenReturn(asistente);
        mvc.perform(post("/asistentes/mi-perfil").with(csrf())
                        .param("nombres", "Ana").param("apellidos", "Torres"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("mensaje"))
                .andExpect(content().string(containsString("No se pudo guardar el perfil")))
                .andExpect(content().string(not(containsString("error de base de datos"))));
    }

    @Test
    @WithMockUser(username = "ana@example.com", roles = "ASISTENTE")
    void postSinCsrfEsRechazado() throws Exception {
        mvc.perform(post("/asistentes/mi-perfil")
                        .param("nombres", "Ana").param("apellidos", "Torres"))
                .andExpect(status().isForbidden());
        verify(asistenteServicio, never()).actualizarPerfilPropio(any(), any());
    }

    @Test
    @WithMockUser(username = "ana@example.com", roles = "ASISTENTE")
    void formularioInvalidoNoGuardaNiMuestraExito() throws Exception {
        when(asistenteServicio.buscarPerfilPropio("ana@example.com")).thenReturn(asistente);
        mvc.perform(post("/asistentes/mi-perfil").with(csrf())
                        .param("nombres", " ").param("apellidos", "Torres").param("edad", "15"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("perfil", "nombres", "edad"))
                .andExpect(content().string(containsString("Revisa los campos")));
        verify(asistenteServicio, never()).actualizarPerfilPropio(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorNoPuedeEditarPerfilDeAsistente() throws Exception {
        mvc.perform(post("/asistentes/mi-perfil").with(csrf())
                        .param("nombres", "Admin").param("apellidos", "Otro"))
                .andExpect(status().isForbidden());
        verify(asistenteServicio, never()).actualizarPerfilPropio(any(), any());
    }

    @Test
    void sinSesionNoPuedeAbrirPerfil() throws Exception {
        mvc.perform(get("/asistentes/mi-perfil"))
                .andExpect(status().is3xxRedirection());
    }
}
