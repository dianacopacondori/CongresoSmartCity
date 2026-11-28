package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.Reporte;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.modelo.enums.TipoReporte;
import com.congreso.seguridad.ConfiguracionSeguridad;
import com.congreso.servicio.ReporteServicio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteServlet.class)
@Import(ConfiguracionSeguridad.class)
@WithMockUser(username = "admin@example.invalid", roles = "ADMINISTRADOR")
class ReporteExportacionVistaTest {

    @Autowired private MockMvc mvc;
    @MockBean private UsuarioDAO usuarioDAO;
    @MockBean private ReporteServicio reporteServicio;

    private MockHttpSession sesionAdmin() {
        MockHttpSession sesion = new MockHttpSession();
        Administrador administrador = new Administrador();
        administrador.setRol(RolUsuario.ADMINISTRADOR);
        sesion.setAttribute("usuario", administrador);
        return sesion;
    }

    private Reporte reporteConFilas() {
        Reporte reporte = new Reporte();
        reporte.setTipo(TipoReporte.PAGOS);
        reporte.setResultados(List.of("fila-1"));
        reporte.setFiltros("{fechaInicio=2026-11-01}");
        reporte.setFechaGeneracion(LocalDateTime.of(2026, 11, 22, 10, 0));
        return reporte;
    }

    @Test
    void resultadoMuestraFiltrosYEnlacesDeDescargaConTodosLosFiltros() throws Exception {
        when(reporteServicio.generarReporte(eq("PAGOS"), any(LocalDate.class), any(LocalDate.class), eq(4L)))
                .thenReturn(reporteConFilas());

        mvc.perform(get("/reportes/admin/generar")
                        .session(sesionAdmin())
                        .param("tipo", "PAGOS")
                        .param("fechaInicio", "2026-11-01")
                        .param("fechaFin", "2026-11-30")
                        .param("actividadId", "4"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fechaInicio=2026-11-01")))
                .andExpect(content().string(containsString("fechaFin=2026-11-30")))
                .andExpect(content().string(containsString("actividadId=4")))
                .andExpect(content().string(containsString("Descargar Excel")))
                .andExpect(content().string(containsString("Descargar PDF")));

        verify(reporteServicio).guardarReporte(any());
    }

    @Test
    void sinResultadosNoOfreceDescargas() throws Exception {
        Reporte reporte = reporteConFilas();
        reporte.setResultados(List.of());
        when(reporteServicio.generarReporte(eq("PAGOS"), isNull(), isNull(), isNull())).thenReturn(reporte);

        mvc.perform(get("/reportes/admin/generar").session(sesionAdmin()).param("tipo", "PAGOS"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron resultados.")))
                .andExpect(content().string(not(containsString("Descargar Excel"))));
    }

    @Test
    void errorDeValidacionVuelveAlMenuConOpcionesYFiltros() throws Exception {
        when(reporteServicio.generarReporte(eq("PAGOS"), any(LocalDate.class), any(LocalDate.class), isNull()))
                .thenThrow(new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin"));

        mvc.perform(get("/reportes/admin/generar")
                        .session(sesionAdmin())
                        .param("tipo", "PAGOS")
                        .param("fechaInicio", "2026-11-30")
                        .param("fechaFin", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La fecha de inicio no puede ser posterior")))
                .andExpect(content().string(containsString("ASISTENCIAS")))
                .andExpect(content().string(containsString("value=\"2026-11-30\"")));

        verify(reporteServicio, never()).guardarReporte(any());
    }

    @Test
    void fechaMalFormadaRecuperaVistaYConservaTodosLosFiltros() throws Exception {
        mvc.perform(get("/reportes/admin/generar")
                        .session(sesionAdmin())
                        .param("tipo", "PAGOS")
                        .param("fechaInicio", "fecha-invalida")
                        .param("fechaFin", "2026-11-30")
                        .param("actividadId", "4"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tiposReporte", org.hamcrest.Matchers.hasSize(6)))
                .andExpect(model().attribute("tipoSeleccionado", "PAGOS"))
                .andExpect(model().attribute("fechaInicio", "fecha-invalida"))
                .andExpect(model().attribute("fechaFin", "2026-11-30"))
                .andExpect(model().attribute("actividadId", "4"))
                .andExpect(content().string(containsString("fecha-invalida")))
                .andExpect(content().string(containsString("ASISTENCIAS")));

        verify(reporteServicio, never()).generarReporte(any(), any(), any(), any());
    }

    @Test
    void tipoInvalidoRecuperaOpcionesYFiltros() throws Exception {
        when(reporteServicio.generarReporte(eq("NO_EXISTE"), any(LocalDate.class), any(LocalDate.class), eq(4L)))
                .thenThrow(new IllegalArgumentException("Tipo de reporte no válido"));

        mvc.perform(get("/reportes/admin/generar")
                        .session(sesionAdmin())
                        .param("tipo", "NO_EXISTE")
                        .param("fechaInicio", "2026-11-01")
                        .param("fechaFin", "2026-11-30")
                        .param("actividadId", "4"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tiposReporte", org.hamcrest.Matchers.hasSize(6)))
                .andExpect(model().attribute("tipoSeleccionado", "NO_EXISTE"))
                .andExpect(model().attribute("fechaInicio", "2026-11-01"))
                .andExpect(model().attribute("fechaFin", "2026-11-30"))
                .andExpect(model().attribute("actividadId", "4"))
                .andExpect(content().string(containsString("Tipo de reporte no válido")))
                .andExpect(content().string(containsString("value=\"NO_EXISTE\"")));
    }

    @Test
    void actividadInexistenteRecuperaOpcionesYFiltros() throws Exception {
        when(reporteServicio.generarReporte(eq("ACTIVIDADES"), isNull(), isNull(), eq(999L)))
                .thenThrow(new IllegalArgumentException("La actividad indicada no existe"));

        mvc.perform(get("/reportes/admin/generar")
                        .session(sesionAdmin())
                        .param("tipo", "ACTIVIDADES")
                        .param("actividadId", "999"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tiposReporte", org.hamcrest.Matchers.hasSize(6)))
                .andExpect(model().attribute("tipoSeleccionado", "ACTIVIDADES"))
                .andExpect(model().attribute("actividadId", "999"))
                .andExpect(content().string(containsString("La actividad indicada no existe")))
                .andExpect(content().string(containsString("ASISTENCIAS")))
                .andExpect(content().string(containsString("value=\"999\"")));
    }

    @Test
    void exportacionExcelDevuelveArchivoYCacheDesactivada() throws Exception {
        byte[] archivo = new byte[]{'P', 'K', 3, 4, 0, 0};
        when(reporteServicio.generarReporte(eq("PAGOS"), isNull(), isNull(), isNull()))
                .thenReturn(reporteConFilas());
        when(reporteServicio.exportarReporteExcel(any())).thenReturn(archivo);

        mvc.perform(get("/reportes/admin/exportar-excel").session(sesionAdmin()).param("tipo", "PAGOS"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition", containsString("attachment;")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().bytes(archivo));
    }

    @Test
    void exportacionPdfDevuelveArchivo() throws Exception {
        byte[] archivo = "%PDF-1.4\n%%EOF".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        when(reporteServicio.generarReporte(eq("PAGOS"), isNull(), isNull(), isNull()))
                .thenReturn(reporteConFilas());
        when(reporteServicio.exportarReportePDF(any())).thenReturn(archivo);

        mvc.perform(get("/reportes/admin/exportar-pdf").session(sesionAdmin()).param("tipo", "PAGOS"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("attachment;")))
                .andExpect(header().string("Content-Disposition", containsString(".pdf")))
                .andExpect(content().bytes(archivo));
    }

    @Test
    void historialMuestraReportesGuardados() throws Exception {
        when(reporteServicio.obtenerTodosLosReportes()).thenReturn(List.of(reporteConFilas()));

        mvc.perform(get("/reportes/admin/listar").session(sesionAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Historial de reportes")))
                .andExpect(content().string(not(containsString("Aún no hay reportes generados."))));
    }

    @Test
    @WithMockUser(username = "asistente@example.invalid", roles = "ASISTENTE")
    void exportacionesExigenAdministrador() throws Exception {
        mvc.perform(get("/reportes/admin/exportar-excel").param("tipo", "PAGOS"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reportes/admin/exportar-pdf").param("tipo", "PAGOS"))
                .andExpect(status().isForbidden());
        verify(reporteServicio, never()).exportarReporteExcel(any());
        verify(reporteServicio, never()).exportarReportePDF(any());
    }
}
