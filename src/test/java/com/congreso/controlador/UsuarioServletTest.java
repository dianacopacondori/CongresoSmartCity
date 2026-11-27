package com.congreso.controlador;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.UsuarioServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UsuarioServletTest {

    private UsuarioServicio usuarioServicio;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        usuarioServicio = mock(UsuarioServicio.class);
        mvc = standaloneSetup(new UsuarioServlet(usuarioServicio))
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/views/", ".jsp"))
                .build();
    }

    @Test
    void formularioDeRegistroUsaAsistente() throws Exception {
        mvc.perform(get("/usuarios/registro"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attribute("usuario", org.hamcrest.Matchers.instanceOf(Asistente.class)));
    }

    @Test
    void registroIgnoraRolEIdEnviadosPorElCliente() throws Exception {
        mvc.perform(post("/usuarios/registro")
                        .param("nombres", "Ana")
                        .param("apellidos", "Pérez")
                        .param("identificacion", "12345678")
                        .param("correo", "ana@example.com")
                        .param("contrasena", "password123")
                        .param("rol", "ADMINISTRADOR")
                        .param("codigo", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/login"));

        ArgumentCaptor<Asistente> captor = ArgumentCaptor.forClass(Asistente.class);
        verify(usuarioServicio).registrarUsuario(captor.capture());
        assertInstanceOf(Asistente.class, captor.getValue());
        assertEquals(RolUsuario.ASISTENTE, captor.getValue().getRol());
        assertNull(captor.getValue().getCodigo());
    }
}
