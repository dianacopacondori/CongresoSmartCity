package com.congreso.controlador;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.AsistenteServicio;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.servicio.PagoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AsistenteServlet.class, InscripcionServlet.class})
@Import(ConfiguracionSeguridad.class)
class BajaAdministrativaVistaTest {

    private static final String ADMIN = "admin@example.invalid";

    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarioDAO;
    @MockBean private AsistenteDAO asistenteDAO;
    @MockBean private AsistenteServicio asistenteServicio;
    @MockBean private InscripcionServicio inscripcionServicio;
    @MockBean private PagoServicio pagoServicio;

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void bajaDeAsistenteYaNoFuncionaPorGet() throws Exception {
        mvc.perform(get("/asistentes/admin/eliminar/7"))
                .andExpect(status().isMethodNotAllowed());
        verify(asistenteServicio, never()).eliminarAsistente(eq(7L), eq(ADMIN));
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void bajaDeAsistenteRequiereCsrf() throws Exception {
        mvc.perform(post("/asistentes/admin/eliminar/7"))
                .andExpect(status().isForbidden());
        verify(asistenteServicio, never()).eliminarAsistente(eq(7L), eq(ADMIN));
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void bajaDeAsistenteConPostYCsrfLlamaAlServicio() throws Exception {
        mvc.perform(post("/asistentes/admin/eliminar/7").with(csrf()))
                .andExpect(redirectedUrl("/asistentes/admin/listar"));
        verify(asistenteServicio).eliminarAsistente(7L, ADMIN);
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void cancelacionDeInscripcionYaNoFuncionaPorGet() throws Exception {
        mvc.perform(get("/inscripciones/admin/eliminar/11"))
                .andExpect(status().isMethodNotAllowed());
        verify(inscripcionServicio, never()).cancelarInscripcion(eq(11L), eq(ADMIN));
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void cancelacionDeInscripcionRequiereCsrf() throws Exception {
        mvc.perform(post("/inscripciones/admin/eliminar/11"))
                .andExpect(status().isForbidden());
        verify(inscripcionServicio, never()).cancelarInscripcion(eq(11L), eq(ADMIN));
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void cancelacionDeInscripcionConPostYCsrfLlamaAlServicio() throws Exception {
        mvc.perform(post("/inscripciones/admin/eliminar/11").with(csrf()))
                .andExpect(redirectedUrl("/inscripciones/admin/listar"));
        verify(inscripcionServicio).cancelarInscripcion(11L, ADMIN);
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMINISTRADOR")
    void listadosOfrecenAccionesPostConToken() throws Exception {
        Asistente asistente = new Asistente();
        asistente.setCodigo(7L);
        asistente.setNombres("Ana");
        asistente.setApellidos("Pérez");
        asistente.setEstado(EstadoUsuario.ACTIVO);
        when(asistenteServicio.obtenerTodosLosAsistentes()).thenReturn(List.of(asistente));

        Inscripcion inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.setCodigo(11L);
        inscripcion.setEstado(EstadoInscripcion.PENDIENTE);
        when(inscripcionServicio.listarTodasLasInscripciones()).thenReturn(List.of(inscripcion));

        mvc.perform(get("/asistentes/admin/listar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/asistentes/admin/eliminar/7\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));

        mvc.perform(get("/inscripciones/admin/listar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/inscripciones/admin/eliminar/11\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void lasRutasDeBajaExigenAdministrador() throws Exception {
        mvc.perform(post("/asistentes/admin/eliminar/7").with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/inscripciones/admin/eliminar/11").with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(asistenteServicio, never()).eliminarAsistente(eq(7L), org.mockito.ArgumentMatchers.any());
        verify(inscripcionServicio, never()).cancelarInscripcion(eq(11L), org.mockito.ArgumentMatchers.any());
    }
}
