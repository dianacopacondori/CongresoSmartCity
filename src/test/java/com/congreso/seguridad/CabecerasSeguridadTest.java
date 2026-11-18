package com.congreso.seguridad;

import com.congreso.controlador.CongresoController;
import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.AsistenciaServicio;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.servicio.PagoServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de regresion de los controles de seguridad verificados en la auditoria.
 */
@WebMvcTest(controllers = CongresoController.class)
@Import(ConfiguracionSeguridad.class)
class CabecerasSeguridadTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UsuarioDAO usuarioDAO;
    @MockBean
    private ActividadServicio actividadServicio;
    @MockBean
    private ColaboradorServicio colaboradorServicio;
    @MockBean
    private InscripcionServicio inscripcionServicio;
    @MockBean
    private AsistenciaServicio asistenciaServicio;
    @MockBean
    private PagoServicio pagoServicio;

    @BeforeEach
    void prepararDatosPublicos() {
        when(actividadServicio.obtenerActividadesActivas()).thenReturn(List.of());
        when(colaboradorServicio.listarPonentes()).thenReturn(List.of());
    }

    @Test
    void aplicaContentSecurityPolicyEstricta() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                        containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("object-src 'none'")))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy",
                        not(containsString("unsafe-inline"))))
                .andExpect(header().string("Content-Security-Policy",
                        not(containsString("http://"))))
                .andExpect(header().string("Content-Security-Policy",
                        not(containsString("https://"))));
    }

    @Test
    void aplicaCabecerasDeProteccionAdicionales() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy",
                        containsString("camera=()")));
    }

    @Test
    void aplicaHstsCuandoLaPeticionEsHttps() throws Exception {
        mvc.perform(get("/").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=31536000 ; includeSubDomains"));
    }

    @Test
    void redirigeAlLoginQuienIntentaEntrarAlAreaAdministrativaSinAutenticarse() throws Exception {
        mvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/usuarios/login"));
    }

    @Test
    @WithMockUser(roles = "ASISTENTE")
    void rechazaUsuarioAutenticadoSinRolAdministrador() throws Exception {
        mvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaPeticionesDeCambioDeEstadoSinTokenCsrf() throws Exception {
        mvc.perform(post("/usuarios/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unLoginValidoCreaLaSesionYRedirigeSegunElRol() throws Exception {
        Administrador administrador = new Administrador();
        administrador.setCorreo("admin@example.com");
        administrador.setContrasena(new BCryptPasswordEncoder().encode("ClaveSegura123!"));
        administrador.setRol(RolUsuario.ADMINISTRADOR);
        when(usuarioDAO.findByCorreo(anyString())).thenReturn(Optional.of(administrador));

        mvc.perform(post("/usuarios/login")
                        .param("correo", "admin@example.com")
                        .param("contrasena", "ClaveSegura123!")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(request().sessionAttribute("rol", RolUsuario.ADMINISTRADOR));
    }

    @Test
    void credencialesInvalidasNoCreanSesionAutenticada() throws Exception {
        when(usuarioDAO.findByCorreo(anyString())).thenReturn(Optional.empty());

        mvc.perform(post("/usuarios/login")
                        .param("correo", "admin@example.com")
                        .param("contrasena", "contrasena-erronea")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/login?error"))
                .andExpect(request().sessionAttributeDoesNotExist("usuario"));
    }
}
