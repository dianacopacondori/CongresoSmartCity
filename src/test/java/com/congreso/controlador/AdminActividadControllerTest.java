package com.congreso.controlador;

import com.congreso.dto.ActividadFormulario;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.SalaServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AdminActividadControllerTest {

    private ActividadServicio actividadServicio;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        actividadServicio = mock(ActividadServicio.class);
        SalaServicio salaServicio = mock(SalaServicio.class);
        ColaboradorServicio colaboradorServicio = mock(ColaboradorServicio.class);
        when(salaServicio.obtenerTodasLasSalas()).thenReturn(List.of());
        when(colaboradorServicio.listarPonentes()).thenReturn(List.of());
        when(colaboradorServicio.listarInstructores()).thenReturn(List.of());
        mvc = standaloneSetup(new AdminActividadController(actividadServicio, salaServicio, colaboradorServicio))
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/views/", ".jsp"))
                .build();
    }

    @Test
    void listaYFormularioUsanLasRutasYPlantillasAdministrativas() throws Exception {
        when(actividadServicio.listarTodasLasActividades()).thenReturn(List.of());
        mvc.perform(get("/admin/actividades"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin-actividades"))
                .andExpect(model().attribute("actividades", List.of()));
        mvc.perform(get("/admin/actividades/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/actividad-form"))
                .andExpect(model().attribute("form", instanceOf(ActividadFormulario.class)))
                .andExpect(model().attribute("edicion", false));
    }

    @Test
    void formularioValidoEnviaAlServicioYRegresaAlListado() throws Exception {
        mvc.perform(post("/admin/actividades")
                        .param("tipo", "PONENCIA")
                        .param("titulo", "Ciudad inteligente")
                        .param("fecha", "2026-11-20")
                        .param("horarioInicio", "09:00")
                        .param("horarioFin", "10:00")
                        .param("cupo", "20")
                        .param("salaId", "1")
                        .param("colaboradorId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/actividades"));
        verify(actividadServicio).crearDesdeFormulario(any(ActividadFormulario.class));
    }

    @Test
    void formularioInvalidoRetieneLaVistaYLosDatosDeSeleccion() throws Exception {
        mvc.perform(post("/admin/actividades").param("tipo", "PONENCIA"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/actividad-form"))
                .andExpect(model().attribute("salas", List.of()))
                .andExpect(model().attribute("edicion", false));
    }
}
