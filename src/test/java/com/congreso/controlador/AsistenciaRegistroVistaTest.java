package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.AsistenciaServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(AsistenciaServlet.class)
@Import(ConfiguracionSeguridad.class)
class AsistenciaRegistroVistaTest {

    private static final String ADMIN = "admin@example.invalid";

    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarioDAO;
    @MockBean private AsistenciaServicio asistenciaServicio;

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void formularioPideElCodigoDelFotocheck() throws Exception {
        mvc.perform(get("/asistencias/registrar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"codigoFotocheck\"")))
                .andExpect(content().string(containsString("name=\"mecanismoValidacion\"")));
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void registroValidoLlamaAlServicioConRegistradorYCodigo() throws Exception {
        mvc.perform(post("/asistencias/registrar").with(csrf())
                        .param("codigoAsistente", "1")
                        .param("codigoActividad", "2")
                        .param("mecanismoValidacion", "CODIGO")
                        .param("codigoFotocheck", "3"))
                .andExpect(redirectedUrl("/asistencias/registrar"))
                .andExpect(flash().attribute("mensaje", "Asistencia registrada y verificada"));
        verify(asistenciaServicio).registrarAsistencia(1L, 2L, "CODIGO", 3L, ADMIN);
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void errorDelServicioNoAnunciaExito() throws Exception {
        when(asistenciaServicio.registrarAsistencia(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("El fotocheck presentado no es válido"));

        mvc.perform(post("/asistencias/registrar").with(csrf())
                        .param("codigoAsistente", "1")
                        .param("codigoActividad", "2")
                        .param("mecanismoValidacion", "CODIGO")
                        .param("codigoFotocheck", "99"))
                .andExpect(redirectedUrl("/asistencias/registrar"))
                .andExpect(flash().attribute("error", "El fotocheck presentado no es válido"));
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void registroRequiereCsrf() throws Exception {
        mvc.perform(post("/asistencias/registrar")
                        .param("codigoAsistente", "1")
                        .param("codigoActividad", "2")
                        .param("mecanismoValidacion", "MANUAL"))
                .andExpect(status().isForbidden());
        verify(asistenciaServicio, never()).registrarAsistencia(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void correccionPasaLaJustificacionYElAdministrador() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        Administrador administrador = new Administrador();
        administrador.setRol(RolUsuario.ADMINISTRADOR);
        sesion.setAttribute("usuario", administrador);

        mvc.perform(post("/asistencias/admin/corregir/20").with(csrf()).session(sesion)
                        .param("justificacion", "Fotocheck verificado en recepción"))
                .andExpect(redirectedUrl("/asistencias/admin/listar"))
                .andExpect(flash().attribute("mensaje", "Asistencia corregida exitosamente"));
        verify(asistenciaServicio).corregirAsistencia(20L, "Fotocheck verificado en recepción", ADMIN);
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void asistenteNoPuedeRegistrarAsistencias() throws Exception {
        mvc.perform(post("/asistencias/registrar").with(csrf())
                        .param("codigoAsistente", "1")
                        .param("codigoActividad", "2")
                        .param("mecanismoValidacion", "MANUAL"))
                .andExpect(status().isForbidden());
        verify(asistenciaServicio, never()).registrarAsistencia(any(), any(), any(), any(), any());
    }
}
