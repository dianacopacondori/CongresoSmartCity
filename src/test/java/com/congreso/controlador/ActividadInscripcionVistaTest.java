package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Sala;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.InscripcionServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActividadServlet.class)
@Import(ConfiguracionSeguridad.class)
class ActividadInscripcionVistaTest {
    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarios;
    @MockBean private ActividadServicio actividades;
    @MockBean private InscripcionServicio inscripciones;

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void formularioInscribirmeSoloSiTieneInscripcionPagada() throws Exception {
        Ponencia actividad = actividad();
        when(actividades.obtenerActividadesActivas()).thenReturn(List.of(actividad));
        Asistente asistente = new Asistente();
        Inscripcion inscripcion = new Inscripcion(asistente, "Estudiante");
        when(inscripciones.inscripcionDisponible("ana@example.invalid")).thenReturn(Optional.of(inscripcion));
        mvc.perform(get("/actividades"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/actividades/inscribir/30\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));

        inscripcion.getActividades().add(actividad);
        mvc.perform(get("/actividades"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya inscrito")));

        when(inscripciones.inscripcionDisponible("ana@example.invalid")).thenReturn(Optional.empty());
        mvc.perform(get("/actividades"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("action=\"/actividades/inscribir/30\""))))
                .andExpect(content().string(containsString("Consulta tu siguiente paso")));
    }

    @Test
    void visitanteNoPuedeEnviarInscripcion() throws Exception {
        when(actividades.obtenerActividadesActivas()).thenReturn(List.of(actividad()));
        mvc.perform(get("/actividades"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("action=\"/actividades/inscribir/30\""))));
        mvc.perform(post("/actividades/inscribir/30").with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(inscripciones, never()).inscribirActividadPropia(any(), any());
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void soloMuestraExitoDespuesDeOperacionReal() throws Exception {
        when(inscripciones.inscribirActividadPropia("ana@example.invalid", 30L))
                .thenThrow(new IllegalStateException("Sin cupos"));
        mvc.perform(post("/actividades/inscribir/30").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/actividades"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attributeCount(1));

        when(inscripciones.inscribirActividadPropia("ana@example.invalid", 31L)).thenReturn(true);
        mvc.perform(post("/actividades/inscribir/31").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("mensaje"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorNoPuedeInscribirseComoAsistente() throws Exception {
        mvc.perform(post("/actividades/inscribir/30").with(csrf()))
                .andExpect(status().isForbidden());
        verify(inscripciones, never()).inscribirActividadPropia(any(), any());
    }

    @Test
    @WithMockUser(roles = "ASISTENTE")
    void postSinCsrfEsRechazado() throws Exception {
        mvc.perform(post("/actividades/inscribir/30"))
                .andExpect(status().isForbidden());
        verify(inscripciones, never()).inscribirActividadPropia(any(), any());
    }

    private Ponencia actividad() {
        Ponencia actividad = new Ponencia();
        actividad.setCodigo(30L);
        actividad.setTitulo("Ponencia de prueba");
        actividad.setCupo(5);
        actividad.setInscritos(0);
        actividad.setEstado(EstadoActividad.ACTIVA);
        actividad.setSala(new Sala("Sala de prueba", 5, "Local"));
        Colaborador ponente = new Colaborador();
        ponente.setNombres("Ponente");
        ponente.setApellidos("Prueba");
        actividad.setPonente(ponente);
        return actividad;
    }
}
