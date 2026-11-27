package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Fotocheck;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.enums.EstadoFotocheck;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.FotocheckServicio;
import com.congreso.servicio.InscripcionServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FotocheckController.class)
@Import(ConfiguracionSeguridad.class)
class FotocheckVistaTest {

    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarioDAO;
    @MockBean private FotocheckServicio fotocheckServicio;
    @MockBean private InscripcionServicio inscripcionServicio;

    private Asistente asistente() {
        Asistente asistente = new Asistente();
        asistente.setCorreo("ana@example.invalid");
        asistente.setNombres("Ana");
        asistente.setApellidos("Pérez");
        asistente.setRol(RolUsuario.ASISTENTE);
        return asistente;
    }

    private Fotocheck fotocheck(Asistente asistente) {
        Fotocheck fotocheck = new Fotocheck(asistente);
        fotocheck.setCodigo(12L);
        return fotocheck;
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void asistenteElegibleVeSuFotocheck() throws Exception {
        Asistente asistente = asistente();
        when(usuarioDAO.findByCorreo(asistente.getCorreo())).thenReturn(Optional.of(asistente));
        when(inscripcionServicio.inscripcionDisponible(asistente.getCorreo()))
                .thenReturn(Optional.of(new Inscripcion(asistente, "Estudiante")));
        when(fotocheckServicio.generarFotocheck(asistente)).thenReturn(fotocheck(asistente));

        mvc.perform(get("/fotocheck/mi-fotocheck"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Código 12")))
                .andExpect(content().string(containsString("Descargar PDF")));
        verify(fotocheckServicio).generarFotocheck(asistente);
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void sinInscripcionPagadaNoSeEmiteFotocheck() throws Exception {
        Asistente asistente = asistente();
        when(usuarioDAO.findByCorreo(asistente.getCorreo())).thenReturn(Optional.of(asistente));
        when(inscripcionServicio.inscripcionDisponible(asistente.getCorreo())).thenReturn(Optional.empty());

        mvc.perform(get("/fotocheck/mi-fotocheck"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("inscripción confirmada con el pago simulado completo")));
        verify(fotocheckServicio, never()).generarFotocheck(any());
    }

    @Test
    @WithMockUser(username = "ana@example.invalid", roles = "ASISTENTE")
    void descargaDevuelvePdfConNombreSeguro() throws Exception {
        Asistente asistente = asistente();
        when(usuarioDAO.findByCorreo(asistente.getCorreo())).thenReturn(Optional.of(asistente));
        when(inscripcionServicio.inscripcionDisponible(asistente.getCorreo()))
                .thenReturn(Optional.of(new Inscripcion(asistente, "Estudiante")));
        when(fotocheckServicio.generarFotocheck(asistente)).thenReturn(fotocheck(asistente));

        mvc.perform(get("/fotocheck/descargar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("fotocheck_12.pdf")))
                .andExpect(content().string(containsString("%PDF-1.4")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminPuedeVerificarUnCodigoEmitido() throws Exception {
        Asistente asistente = asistente();
        Fotocheck fotocheck = fotocheck(asistente);
        when(fotocheckServicio.buscarPorCodigo(12L)).thenReturn(Optional.of(fotocheck));
        when(fotocheckServicio.validarFotocheck(12L)).thenReturn(true);

        mvc.perform(post("/fotocheck/admin/verificar").with(csrf()).param("codigo", "12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("FOTOCHECK VÁLIDO")))
                .andExpect(content().string(containsString("Ana Pérez")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void codigoDesconocidoSeMuestraComoNoValido() throws Exception {
        when(fotocheckServicio.buscarPorCodigo(99L)).thenReturn(Optional.empty());

        mvc.perform(post("/fotocheck/admin/verificar").with(csrf()).param("codigo", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("FOTOCHECK NO VÁLIDO")))
                .andExpect(content().string(containsString("no corresponde")));
    }

    @Test
    @WithMockUser(roles = "ASISTENTE")
    void asistenteNoPuedeUsarLaVerificacionAdministrativa() throws Exception {
        mvc.perform(post("/fotocheck/admin/verificar").with(csrf()).param("codigo", "12"))
                .andExpect(status().isForbidden());
        verify(fotocheckServicio, never()).validarFotocheck(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminNoPuedeEmitirFotocheckPropio() throws Exception {
        mvc.perform(get("/fotocheck/mi-fotocheck"))
                .andExpect(status().isForbidden());
        verify(fotocheckServicio, never()).generarFotocheck(any());
    }
}
