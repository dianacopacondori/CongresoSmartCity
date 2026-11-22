package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Colaborador;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActividadServlet.class)
@Import(ConfiguracionSeguridad.class)
class ActividadFiltrosVistaTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 26);

    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarios;
    @MockBean private ActividadServicio actividades;
    @MockBean private InscripcionServicio inscripciones;

    @Test
    void filtrosSeEnvianAlServicioYSeConservanEnElFormulario() throws Exception {
        Ponencia ponencia = new Ponencia();
        ponencia.setCodigo(11L);
        ponencia.setTitulo("Movilidad sostenible");
        ponencia.setEstado(EstadoActividad.ACTIVA);
        ponencia.setCupo(10);
        ponencia.setInscritos(0);
        ponencia.setSala(new Sala("Sala A", 10, "Local"));
        Colaborador ponente = new Colaborador();
        ponente.setNombres("Ana");
        ponente.setApellidos("Pérez");
        ponencia.setPonente(ponente);
        when(actividades.obtenerActividadesFiltradas("movilidad", "PONENCIA", FECHA))
                .thenReturn(List.of(ponencia));

        mvc.perform(get("/actividades")
                        .param("nombre", "movilidad")
                        .param("tipo", "PONENCIA")
                        .param("fecha", "2026-11-26"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/actividades\"")))
                .andExpect(content().string(containsString("name=\"nombre\"")))
                .andExpect(content().string(containsString("value=\"movilidad\"")))
                .andExpect(content().string(containsString("value=\"2026-11-26\"")))
                .andExpect(content().string(containsString("Movilidad sostenible")))
                .andExpect(content().string(containsString("Limpiar filtros")));
    }

    @Test
    void sinResultadosMuestraEstadoVacioDeFiltros() throws Exception {
        when(actividades.obtenerActividadesFiltradas(any(), any(), any())).thenReturn(List.of());

        mvc.perform(get("/actividades").param("nombre", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sin resultados para estos filtros")))
                .andExpect(content().string(not(containsString("Estamos preparando el programa"))));
    }

    @Test
    void tipoInvalidoMuestraErrorSinFallo500() throws Exception {
        when(actividades.obtenerActividadesFiltradas(eq(""), eq("OTRO"), any()))
                .thenThrow(new IllegalArgumentException("Tipo de actividad no válido"));

        mvc.perform(get("/actividades").param("tipo", "OTRO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tipo de actividad no válido")));
    }

    @Test
    void catalogoEsPublico() throws Exception {
        when(actividades.obtenerActividadesActivas()).thenReturn(List.of());

        mvc.perform(get("/actividades"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Estamos preparando el programa")));
    }
}
