package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.dto.EstadoParticipacion;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.seguridad.ConfiguracionSeguridad;
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

@WebMvcTest(PagoServlet.class)
@Import(ConfiguracionSeguridad.class)
class PagoSimuladoVistaTest {
    @Autowired private MockMvc mvc;
    @MockBean private PagoServicio pagoServicio;
    @MockBean private UsuarioDAO usuarioDAO;

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void vistaMuestraSoloInscripcionesYPagosPropios() throws Exception {
        Asistente asistente = new Asistente();
        asistente.setCorreo("ana@example.invalid");
        Inscripcion inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.setCodigo(7L);
        when(pagoServicio.inscripcionesPendientesPropias("ana@example.invalid"))
                .thenReturn(List.of(inscripcion));
        when(pagoServicio.estadoParticipacion("ana@example.invalid"))
                .thenReturn(new EstadoParticipacion("PENDIENTE", "SIN PAGOS",
                        EstadoParticipacion.Paso.SIMULAR_PAGO, 7L));
        when(pagoServicio.pagosPropios("ana@example.invalid")).thenReturn(List.of());
        mvc.perform(get("/pagos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Simular pago")))
                .andExpect(content().string(containsString("S/ 50")));
        mvc.perform(get("/pagos/mis-pagos")).andExpect(status().isOk());
        verify(pagoServicio).pagosPropios("ana@example.invalid");
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void exitoSoloTrasGuardarYErrorNoRedirigeAConfirmacion() throws Exception {
        when(pagoServicio.simularPago("ana@example.invalid", 7L, ModalidadPago.COMPLETO))
                .thenThrow(new IllegalStateException("Pago duplicado"));
        mvc.perform(post("/pagos/nuevo").with(csrf()).param("codigoInscripcion", "7")
                        .param("modalidad", "COMPLETO").param("importe", "0"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pagos/nuevo"))
                .andExpect(flash().attributeExists("error"));

        Pago guardado = new Pago();
        guardado.setSaldo(0);
        guardado.setEstado(com.congreso.modelo.enums.EstadoPago.PAGADO);
        when(pagoServicio.simularPago("ana@example.invalid", 8L, ModalidadPago.COMPLETO))
                .thenReturn(guardado);
        mvc.perform(post("/pagos/nuevo").with(csrf()).param("codigoInscripcion", "8")
                        .param("modalidad", "COMPLETO"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pagos/confirmacion"))
                .andExpect(flash().attributeExists("mensaje"));
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void misPagosMuestraElHistorialIndividualDeCuotas() throws Exception {
        Asistente asistente = new Asistente();
        asistente.setCorreo("ana@example.invalid");
        Inscripcion inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.setCodigo(7L);
        Pago pago = new Pago(asistente, 50, ModalidadPago.CUOTAS, 2);
        pago.setCodigoPago(11L);
        pago.setInscripcion(inscripcion);
        pago.registrarPago(25);
        pago.registrarPago(25);
        when(pagoServicio.pagosPropios("ana@example.invalid")).thenReturn(List.of(pago));

        mvc.perform(get("/pagos/mis-pagos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Historial de cuotas")))
                .andExpect(content().string(containsString("Cuota 1: S/ 25.0")))
                .andExpect(content().string(containsString("Cuota 2: S/ 25.0")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminNoPuedeSimularPagosDeAsistentes() throws Exception {
        mvc.perform(post("/pagos/nuevo").with(csrf()).param("codigoInscripcion", "7")
                        .param("modalidad", "COMPLETO"))
                .andExpect(status().isForbidden());
        verify(pagoServicio, never()).simularPago(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ASISTENTE")
    void csrfYAntiguoAbonoAdministrativoEstanCerrados() throws Exception {
        mvc.perform(post("/pagos/nuevo").param("codigoInscripcion", "7")
                        .param("modalidad", "COMPLETO"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/pagos/admin/registrar-cuota/1").with(csrf()).param("monto", "999"))
                .andExpect(status().isForbidden());
    }
}
