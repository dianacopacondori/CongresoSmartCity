package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Sala;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.SalaServicio;
import com.congreso.seguridad.ConfiguracionSeguridad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminActividadController.class)
@Import(ConfiguracionSeguridad.class)
class AdminActividadVistaTest {

    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarioDAO;
    @MockBean private ActividadServicio actividadServicio;
    @MockBean private SalaServicio salaServicio;
    @MockBean private ColaboradorServicio colaboradorServicio;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void listadoYFormularioSeRenderizanConThymeleaf() throws Exception {
        when(actividadServicio.listarTodasLasActividades()).thenReturn(List.of());
        when(salaServicio.obtenerTodasLasSalas()).thenReturn(List.of());
        when(colaboradorServicio.listarPonentes()).thenReturn(List.of());
        when(colaboradorServicio.listarInstructores()).thenReturn(List.of());

        mvc.perform(get("/admin/actividades"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Programación del congreso")));
        mvc.perform(get("/admin/actividades/nueva"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear actividad")));
    }

    @Test
    void listadoRequiereAutenticacion() throws Exception {
        mvc.perform(get("/admin/actividades"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ASISTENTE")
    void asistenteNoPuedeAbrirElListadoAdministrativo() throws Exception {
        mvc.perform(get("/admin/actividades"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void formularioDeEdicionUsaElIdentificadorDeLaRuta() throws Exception {
        Ponencia ponencia = new Ponencia();
        ponencia.setCodigo(7L);
        ponencia.setTitulo("Ciudades inteligentes");
        Sala sala = new Sala("Principal", 50, "Primer piso");
        sala.setCodigo(1L);
        ponencia.setSala(sala);
        Colaborador ponente = new Colaborador();
        ponente.setCodigo(2L);
        ponente.setNombres("Ana");
        ponente.setApellidos("Pérez");
        ponencia.setPonente(ponente);
        when(actividadServicio.buscarPorId(7L)).thenReturn(Optional.of(ponencia));
        when(salaServicio.obtenerTodasLasSalas()).thenReturn(List.of(sala));
        when(colaboradorServicio.listarPonentes()).thenReturn(List.of(ponente));
        when(colaboradorServicio.listarInstructores()).thenReturn(List.of());

        mvc.perform(get("/admin/actividades/editar/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/actividades/editar/7")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void listadoSeRenderizaConUnaPonenciaReal() throws Exception {
        Ponencia ponencia = new Ponencia();
        ponencia.setCodigo(7L);
        ponencia.setTitulo("Ciudades inteligentes");
        ponencia.setFecha(LocalDate.of(2026, 11, 20));
        ponencia.setHorarioInicio(LocalTime.of(9, 0));
        ponencia.setHorarioFin(LocalTime.of(10, 0));
        ponencia.setCupo(20);
        ponencia.setEstado(EstadoActividad.ACTIVA);
        ponencia.setSala(new Sala("Principal", 50, "Primer piso"));
        Colaborador ponente = new Colaborador();
        ponente.setNombres("Ana");
        ponente.setApellidos("Pérez");
        ponencia.setPonente(ponente);
        when(actividadServicio.listarTodasLasActividades()).thenReturn(List.of(ponencia));

        mvc.perform(get("/admin/actividades"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ciudades inteligentes")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ana Pérez")));
    }
}
