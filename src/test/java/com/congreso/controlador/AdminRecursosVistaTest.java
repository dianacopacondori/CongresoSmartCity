package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.dto.ActividadAsignadaVista;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.SalaServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({AdminColaboradorController.class, AdminSalaController.class, ActividadesAsignadasController.class})
@Import(ConfiguracionSeguridad.class)
class AdminRecursosVistaTest {

    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarioDAO;
    @MockBean private ColaboradorServicio colaboradorServicio;
    @MockBean private SalaServicio salaServicio;

    @Test
    @WithMockUser(roles = "ASISTENTE")
    void asistenteNoPuedeAbrirCrudAdministrativo() throws Exception {
        mvc.perform(get("/admin/colaboradores"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rutaSinReglaExplicitaSeDeniega() throws Exception {
        mvc.perform(get("/ruta-sin-regla"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void postSinCsrfSeRechazaAntesDePersistir() throws Exception {
        mvc.perform(post("/admin/colaboradores")
                        .param("nombres", "Ana").param("apellidos", "Ramos")
                        .param("identificacion", "123").param("correo", "ana@example.invalid")
                        .param("tipoColaborador", "PONENTE").param("contrasena", "clave-segura"))
                .andExpect(status().isForbidden());
        verify(colaboradorServicio, never()).registrarColaborador(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void dtoInvalidoSeMuestraSinGuardar() throws Exception {
        mvc.perform(post("/admin/colaboradores").with(csrf())
                        .param("nombres", " ").param("tipoColaborador", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/colaborador-form"))
                .andExpect(model().attributeHasFieldErrors("form", "nombres", "tipoColaborador", "contrasena"));
        verify(colaboradorServicio, never()).registrarColaborador(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void contrasenaMultibyteQueSuperaElLimiteDeBcryptSeRechaza() throws Exception {
        mvc.perform(post("/admin/colaboradores").with(csrf())
                        .param("nombres", "Ana").param("apellidos", "Ramos")
                        .param("identificacion", "123").param("correo", "ana@example.invalid")
                        .param("tipoColaborador", "PONENTE")
                        .param("contrasena", "á".repeat(37)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/colaborador-form"))
                .andExpect(model().attributeHasFieldErrors("form", "contrasena"));
        verify(colaboradorServicio, never()).registrarColaborador(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void altaValidaPersisteYSoloEntoncesConfirma() throws Exception {
        mvc.perform(post("/admin/colaboradores").with(csrf())
                        .param("nombres", "Ana").param("apellidos", "Ramos")
                        .param("identificacion", "123").param("correo", "ana@example.invalid")
                        .param("institucion", "Universidad").param("empresa", "Smart City")
                        .param("tipoColaborador", "PONENTE").param("contrasena", "clave-segura")
                        .param("codigo", "99").param("rol", "ADMINISTRADOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/colaboradores"))
                .andExpect(flash().attribute("mensaje", "Colaborador registrado exitosamente"));
        verify(colaboradorServicio).registrarColaborador(org.mockito.ArgumentMatchers.argThat(c ->
                c.getRol() == RolUsuario.PONENTE && c.getTipoColaborador() == RolUsuario.PONENTE
                        && c.getCodigo() == null && c.getContrasena().startsWith("$2")
                        && "Smart City".equals(c.getEmpresa())));
    }

    @Test
    @WithMockUser(username = "ponente@example.invalid", roles = "PONENTE")
    void ponenteSoloVeSusAsignacionesConIdentidadAutenticada() throws Exception {
        when(colaboradorServicio.listarActividadesAsignadas("ponente@example.invalid", RolUsuario.PONENTE))
                .thenReturn(List.<ActividadAsignadaVista>of());

        mvc.perform(get("/ponente/actividades"))
                .andExpect(status().isOk())
                .andExpect(view().name("colaborador/actividades-asignadas"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No tienes actividades asignadas.")));
        verify(colaboradorServicio).listarActividadesAsignadas("ponente@example.invalid", RolUsuario.PONENTE);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void instructorNoPuedeConsultarLaRutaDePonentes() throws Exception {
        mvc.perform(get("/ponente/actividades"))
                .andExpect(status().isForbidden());
        verify(colaboradorServicio, never()).listarActividadesAsignadas(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void capacidadDeSalaSeValidaEnElServidor() throws Exception {
        mvc.perform(post("/admin/salas").with(csrf())
                        .param("nombre", "Sala A").param("capacidad", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/sala-form"))
                .andExpect(model().attributeHasFieldErrors("form", "capacidad"));
        verify(salaServicio, never()).crearSala(any());
    }
}
