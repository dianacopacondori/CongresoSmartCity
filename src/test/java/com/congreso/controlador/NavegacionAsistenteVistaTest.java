package com.congreso.controlador;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.dto.EstadoParticipacion;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.AsistenciaServicio;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.servicio.PagoServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CongresoController.class, InscripcionServlet.class, PagoServlet.class})
@Import(ConfiguracionSeguridad.class)
class NavegacionAsistenteVistaTest {
    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarios;
    @MockBean private AsistenteDAO asistentes;
    @MockBean private ActividadServicio actividades;
    @MockBean private ColaboradorServicio colaboradores;
    @MockBean private AsistenciaServicio asistencias;
    @MockBean private InscripcionServicio inscripciones;
    @MockBean private PagoServicio pagos;

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void panelOfreceCrearInscripcionCuandoNoHayNinguna() throws Exception {
        preparar(new EstadoParticipacion("SIN INSCRIPCIÓN", "SIN PAGOS",
                EstadoParticipacion.Paso.CREAR_INSCRIPCION, null));
        mvc.perform(get("/dashboard-asistente").session(sesion()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/inscripciones/nueva\"")))
                .andExpect(content().string(not(containsString("Ir a mis pagos y completar cuota"))));
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void despuesDeInscripcionGuiarAPagoSimulado() throws Exception {
        preparar(new EstadoParticipacion("PENDIENTE", "SIN PAGOS",
                EstadoParticipacion.Paso.SIMULAR_PAGO, 7L));
        mvc.perform(get("/inscripciones/confirmacion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/pagos/nuevo\"")))
                .andExpect(content().string(not(containsString("href=\"/inscripciones/mi-agenda\""))));
        mvc.perform(get("/dashboard-asistente").session(sesion()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Simular pago")))
                .andExpect(content().string(containsString("SIN PAGOS")));
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void primeraCuotaMuestraSegundaCuotaYNoPrometeConfirmacion() throws Exception {
        preparar(new EstadoParticipacion("PENDIENTE", "PARCIAL",
                EstadoParticipacion.Paso.COMPLETAR_CUOTA, 7L));
        mvc.perform(get("/pagos/confirmacion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Completar segunda cuota")))
                .andExpect(content().string(not(containsString("href=\"/inscripciones/mi-agenda\""))));
        mvc.perform(get("/pagos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Completarla desde mis pagos")));
        mvc.perform(get("/dashboard-asistente").session(sesion()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ir a mis pagos y completar cuota")));
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void pagoCompletoLlevaALaAgenda() throws Exception {
        preparar(new EstadoParticipacion("CONFIRMADA", "PAGADO",
                EstadoParticipacion.Paso.VER_AGENDA, 7L));
        mvc.perform(get("/pagos/confirmacion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/inscripciones/mi-agenda\"")));
        mvc.perform(get("/dashboard-asistente").session(sesion()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PAGADO")))
                .andExpect(content().string(containsString("La inscripción está confirmada")));
    }

    private void preparar(EstadoParticipacion estado) {
        when(pagos.estadoParticipacion("ana@example.invalid")).thenReturn(estado);
        when(inscripciones.obtenerAgendaPersonalizada(1L)).thenReturn(List.of());
        when(asistencias.listarAsistenciasPorAsistente(1L)).thenReturn(List.of());
        when(pagos.inscripcionesPendientesPropias("ana@example.invalid")).thenReturn(List.of());
    }

    private MockHttpSession sesion() {
        Asistente asistente = new Asistente();
        asistente.setCodigo(1L);
        asistente.setRol(RolUsuario.ASISTENTE);
        asistente.setNombres("Ana");
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute("usuario", asistente);
        return sesion;
    }
}
