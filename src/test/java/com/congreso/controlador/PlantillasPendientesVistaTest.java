package com.congreso.controlador;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Reporte;
import com.congreso.modelo.Pago;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Asistencia;
import com.congreso.modelo.enums.TipoReporte;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.AsistenciaServicio;
import com.congreso.servicio.AsistenteServicio;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.servicio.PagoServicio;
import com.congreso.servicio.ReporteServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.ArrayList;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest({InscripcionServlet.class, PagoServlet.class, AsistenteServlet.class,
        AsistenciaServlet.class, ReporteServlet.class})
@Import(ConfiguracionSeguridad.class)
class PlantillasPendientesVistaTest {
    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarioDAO;
    @MockBean private AsistenteDAO asistenteDAO;
    @MockBean private InscripcionServicio inscripcionServicio;
    @MockBean private PagoServicio pagoServicio;
    @MockBean private AsistenteServicio asistenteServicio;
    @MockBean private AsistenciaServicio asistenciaServicio;
    @MockBean private ReporteServicio reporteServicio;

    @Test
    @WithMockUser(roles = "ASISTENTE")
    void renderizaVistasPersonales() throws Exception {
        Asistente usuario = new Asistente();
        usuario.setCodigo(1L);
        usuario.setRol(RolUsuario.ASISTENTE);
        usuario.setNombres("Ana");
        usuario.setApellidos("Pérez");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", usuario);
        when(asistenteServicio.buscarPerfilPropio("user")).thenReturn(usuario);
        when(inscripcionServicio.obtenerAgendaPersonalizada(1L)).thenReturn(List.of());
        when(asistenciaServicio.listarAsistenciasPorAsistente(1L)).thenReturn(List.of());
        for (String ruta : List.of("/inscripciones/nueva", "/inscripciones/confirmacion",
                "/inscripciones/mi-agenda", "/asistentes/mi-perfil", "/pagos/nuevo",
                "/pagos/confirmacion", "/pagos/mis-pagos", "/asistencias/mis-asistencias")) {
            mvc.perform(get(ruta).session(session)).andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void renderizaVistasAdministrativas() throws Exception {
        Administrador usuario = new Administrador();
        usuario.setRol(RolUsuario.ADMINISTRADOR);
        usuario.setNombres("Administrador");
        usuario.setApellidos("Congreso");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", usuario);
        when(inscripcionServicio.listarTodasLasInscripciones()).thenReturn(List.of());
        when(pagoServicio.obtenerTodosLosPagos()).thenReturn(List.of());
        when(asistenteServicio.obtenerTodosLosAsistentes()).thenReturn(List.of());
        when(asistenciaServicio.obtenerTodasLasAsistencias()).thenReturn(List.of());
        when(reporteServicio.obtenerTodosLosReportes()).thenReturn(List.of());
        for (String ruta : List.of("/inscripciones/admin/listar", "/pagos/admin/listar",
                "/asistentes/admin/listar", "/asistencias/registrar",
                "/asistencias/admin/listar", "/reportes/admin/menu", "/reportes/admin/listar")) {
            mvc.perform(get(ruta).session(session)).andExpect(status().isOk());
        }
        Reporte reporte = new Reporte();
        reporte.setResultados(new ArrayList<>());
        when(reporteServicio.generarReporte("ASISTENTES", null, null, null)).thenReturn(reporte);
        mvc.perform(get("/reportes/admin/generar").param("tipo", "ASISTENTES").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void renderizaListadosConRegistros() throws Exception {
        Administrador usuario = new Administrador();
        usuario.setRol(RolUsuario.ADMINISTRADOR);
        usuario.setNombres("Admin");
        usuario.setApellidos("Congreso");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", usuario);
        Asistente asistente = new Asistente();
        asistente.setCodigo(1L);
        asistente.setNombres("Ana");
        asistente.setApellidos("Pérez");
        asistente.setCorreo("ana@example.com");
        Inscripcion inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.setCodigo(2L);
        Pago pago = new Pago();
        pago.setCodigoPago(3L);
        pago.setAsistente(asistente);
        pago.setInscripcion(inscripcion);
        Ponencia actividad = new Ponencia();
        actividad.setTitulo("Ciudades inteligentes");
        Asistencia asistencia = new Asistencia();
        asistencia.setCodigo(4L);
        asistencia.setAsistente(asistente);
        asistencia.setActividad(actividad);
        Reporte reporte = new Reporte();
        reporte.setCodigo(5L);
        reporte.setTipo(TipoReporte.ASISTENTES);
        reporte.setResultados(List.of("Ana Pérez"));
        when(asistenteServicio.obtenerTodosLosAsistentes()).thenReturn(List.of(asistente));
        when(inscripcionServicio.listarTodasLasInscripciones()).thenReturn(List.of(inscripcion));
        when(pagoServicio.obtenerTodosLosPagos()).thenReturn(List.of(pago));
        when(asistenciaServicio.obtenerTodasLasAsistencias()).thenReturn(List.of(asistencia));
        when(reporteServicio.obtenerTodosLosReportes()).thenReturn(List.of(reporte));
        when(reporteServicio.generarReporte("ASISTENTES", null, null, null)).thenReturn(reporte);
        for (String ruta : List.of("/asistentes/admin/listar", "/inscripciones/admin/listar",
                "/pagos/admin/listar", "/asistencias/admin/listar")) {
            mvc.perform(get(ruta).session(session)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("Ana Pérez")));
        }
        mvc.perform(get("/reportes/admin/listar").session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("ASISTENTES")));
        mvc.perform(get("/reportes/admin/generar").param("tipo", "ASISTENTES").session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Ana Pérez")));
    }
}
