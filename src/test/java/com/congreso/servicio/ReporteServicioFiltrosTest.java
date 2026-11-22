package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenciaDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.dao.ReporteDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistencia;
import com.congreso.modelo.Pago;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Reporte;
import com.congreso.modelo.enums.TipoReporte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReporteServicioFiltrosTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 11, 30);

    private ActividadDAO actividadDAO;
    private AsistenciaDAO asistenciaDAO;
    private PagoDAO pagoDAO;
    private ReporteDAO reporteDAO;
    private ReporteServicio servicio;

    @BeforeEach
    void configurar() {
        actividadDAO = mock(ActividadDAO.class);
        asistenciaDAO = mock(AsistenciaDAO.class);
        pagoDAO = mock(PagoDAO.class);
        reporteDAO = mock(ReporteDAO.class);
        servicio = new ReporteServicio(asistenciaDAO, pagoDAO, mock(UsuarioDAO.class),
                actividadDAO, mock(InscripcionDAO.class), reporteDAO);
    }

    @Test
    void filtraPagosPorRangoDeFechas() {
        when(pagoDAO.findAll()).thenReturn(List.of(
                pago(LocalDate.of(2026, 10, 31)),
                pago(LocalDate.of(2026, 11, 5)),
                pago(LocalDate.of(2026, 11, 20)),
                pago(LocalDate.of(2026, 12, 1))));

        Reporte reporte = servicio.generarReporte("PAGOS", INICIO, FIN, null);

        assertEquals(2, reporte.getResultados().size());
        assertTrue(reporte.getFiltros().contains("2026-11-01"));
        assertTrue(reporte.getFiltros().contains("2026-11-30"));
    }

    @Test
    void filtraActividadesPorCodigo() {
        Actividad primera = actividad(1L, LocalDate.of(2026, 11, 10));
        Actividad segunda = actividad(2L, LocalDate.of(2026, 11, 11));
        segunda.setTitulo("Segunda");
        when(actividadDAO.findAll()).thenReturn(List.of(primera, segunda));
        when(actividadDAO.findById(2L)).thenReturn(Optional.of(segunda));

        Reporte reporte = servicio.generarReporte("ACTIVIDADES", null, null, 2L);

        assertEquals(1, reporte.getResultados().size());
        assertTrue(reporte.getResultados().get(0).contains("Segunda"));
    }

    @Test
    void filtraAsistenciasPorActividad() {
        Actividad primera = actividad(1L, LocalDate.of(2026, 11, 10));
        Actividad segunda = actividad(2L, LocalDate.of(2026, 11, 11));
        when(asistenciaDAO.findAll()).thenReturn(List.of(
                new Asistencia(null, primera, INICIO, null, "MANUAL", null),
                new Asistencia(null, segunda, INICIO, null, "MANUAL", null)));
        when(actividadDAO.findById(1L)).thenReturn(Optional.of(primera));

        Reporte reporte = servicio.generarReporte("ASISTENCIAS", null, null, 1L);

        assertEquals(1, reporte.getResultados().size());
    }

    @Test
    void validaTipoRangoFechasYActividad() {
        assertThrows(IllegalArgumentException.class, () -> servicio.generarReporte("NO_EXISTE", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.generarReporte("PAGOS", FIN, INICIO, null));
        when(actividadDAO.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> servicio.generarReporte("PAGOS", null, null, 99L));
    }

    @Test
    void exportaExcelConContenidoReal() throws Exception {
        Reporte reporte = reporte("PAGOS", List.of("fila-1", "fila-2"));

        byte[] bytes = servicio.exportarReporteExcel(reporte);

        assertTrue(bytes.length > 100);
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
        assertTrue(new String(bytes, StandardCharsets.ISO_8859_1).contains("PK\u0005\u0006"));
        try (var libro = org.apache.poi.ss.usermodel.WorkbookFactory.create(
                new java.io.ByteArrayInputStream(bytes))) {
            assertEquals("Reporte PAGOS", libro.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("fila-1", libro.getSheetAt(0).getRow(4).getCell(0).getStringCellValue());
        }
    }

    @Test
    void cadaComaSeEscribeEnUnaCeldaDistintaDelExcel() throws Exception {
        Reporte reporte = reporte("PAGOS", List.of("Ana,Pérez,ana@example.invalid", "Valor,\"con \"\"comilla, interna\"\"\""));

        byte[] bytes = servicio.exportarReporteExcel(reporte);

        try (var libro = org.apache.poi.ss.usermodel.WorkbookFactory.create(
                new java.io.ByteArrayInputStream(bytes))) {
            var hoja = libro.getSheetAt(0);
            assertEquals(3, hoja.getRow(4).getPhysicalNumberOfCells());
            assertEquals("Ana", hoja.getRow(4).getCell(0).getStringCellValue());
            assertEquals("Pérez", hoja.getRow(4).getCell(1).getStringCellValue());
            assertEquals("ana@example.invalid", hoja.getRow(4).getCell(2).getStringCellValue());
            assertEquals(2, hoja.getRow(5).getPhysicalNumberOfCells());
            assertEquals("Valor", hoja.getRow(5).getCell(0).getStringCellValue());
            assertEquals("con \"comilla, interna\"", hoja.getRow(5).getCell(1).getStringCellValue());
        }
    }

    @Test
    void losResultadosSeGeneranComoFilasConColumnasUtiles() {
        Pago pago = pago(LocalDate.of(2026, 11, 5));
        pago.setCodigoPago(15L);
        pago.setEstado(com.congreso.modelo.enums.EstadoPago.PAGADO);
        when(pagoDAO.findAll()).thenReturn(List.of(pago));

        Reporte reporte = servicio.generarReporte("PAGOS", null, null, null);

        String fila = reporte.getResultados().get(0);
        assertTrue(fila.startsWith("15,2026-11-05,50.0,0.0,PAGADO"));
        assertEquals(8, dividir(fila).size());
    }

    private List<String> dividir(String fila) {
        List<String> columnas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < fila.length(); i++) {
            char c = fila.charAt(i);
            if (c == '"') comillas = !comillas;
            else if (c == ',' && !comillas) { columnas.add(actual.toString()); actual.setLength(0); }
            else actual.append(c);
        }
        columnas.add(actual.toString());
        return columnas;
    }

    @Test
    void exportaPdfValidoYPaginadoCuandoHayMuchasFilas() throws Exception {
        List<String> filas = new ArrayList<>();
        for (int i = 0; i < 60; i++) filas.add("Fila " + i);
        Reporte reporte = reporte("ASISTENCIAS", filas);

        byte[] bytes = servicio.exportarReportePDF(reporte);
        String contenido = new String(bytes, StandardCharsets.ISO_8859_1);

        assertTrue(contenido.startsWith("%PDF-1.4"));
        assertTrue(contenido.trim().endsWith("%%EOF"));
        assertTrue(contenido.contains("/Count 2"));
        assertTrue(contenido.contains("Fila 59"));
        verificarTablaXref(contenido);
    }

    /** Cada entrada xref debe apuntar al inicio real de su objeto. */
    private void verificarTablaXref(String pdf) {
        int marca = pdf.lastIndexOf("startxref");
        assertTrue(marca > 0);
        int inicioXref = Integer.parseInt(pdf.substring(marca + "startxref".length()).trim().split("\\s+")[0]);
        assertTrue(pdf.startsWith("xref", inicioXref));
        String[] lineas = pdf.substring(inicioXref).split("\\R");
        int objetos = Integer.parseInt(lineas[1].split("\\s+")[1]) - 1;
        for (int i = 1; i <= objetos; i++) {
            String entrada = lineas[2 + i];
            int desplazamiento = Integer.parseInt(entrada.substring(0, 10).trim());
            assertTrue(pdf.startsWith(i + " 0 obj", desplazamiento),
                    "La entrada xref " + i + " no apunta a su objeto");
        }
    }

    @Test
    void pdfDivideLasLineasLargasParaQueNoSeCorten() {
        StringBuilder largo = new StringBuilder();
        for (int i = 0; i < 40; i++) largo.append("palabra").append(i).append(' ');
        Reporte reporte = reporte("PAGOS", List.of(largo.toString().trim()));

        byte[] bytes = servicio.exportarReportePDF(reporte);
        String pdf = new String(bytes, StandardCharsets.ISO_8859_1);

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\(([^()]*)\\) Tj").matcher(pdf);
        int longitudMaxima = 0;
        int fragmentos = 0;
        while (matcher.find()) {
            longitudMaxima = Math.max(longitudMaxima, matcher.group(1).length());
            fragmentos++;
        }
        assertTrue(longitudMaxima <= 90, "Una línea del PDF mide " + longitudMaxima + " caracteres");
        assertTrue(fragmentos > 6);
        assertTrue(pdf.contains("palabra0"));
        assertTrue(pdf.contains("palabra39"));
    }

    @Test
    void exportacionSinResultadosNoEstaVaciaYLoIndica() {
        Reporte reporte = reporte("PAGOS", List.of());

        byte[] excel = servicio.exportarReporteExcel(reporte);
        byte[] pdf = servicio.exportarReportePDF(reporte);

        assertTrue(excel.length > 0);
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).contains("No se encontraron resultados."));
    }

    @Test
    void historialYGuardadoSeDelegadosAlRepositorio() {
        Reporte reporte = reporte("PAGOS", List.of("fila"));
        List<Reporte> esperados = List.of(reporte);
        when(reporteDAO.findAllByOrderByFechaGeneracionDesc()).thenReturn(esperados);
        when(reporteDAO.saveAndFlush(any())).thenReturn(reporte);

        assertEquals(esperados, servicio.obtenerTodosLosReportes());
        assertEquals(reporte, servicio.guardarReporte(reporte));
        verify(reporteDAO).saveAndFlush(reporte);
    }

    private static Pago pago(LocalDate fecha) {
        Pago pago = new Pago();
        pago.setFechaPago(fecha);
        pago.setImporte(50.0);
        pago.setSaldo(0.0);
        return pago;
    }

    private static Actividad actividad(Long codigo, LocalDate fecha) {
        Actividad actividad = new Ponencia();
        actividad.setCodigo(codigo);
        actividad.setFecha(fecha);
        return actividad;
    }

    private static Reporte reporte(String tipo, List<String> filas) {
        Reporte reporte = new Reporte();
        reporte.setTipo(TipoReporte.valueOf(tipo));
        reporte.setResultados(filas);
        reporte.setFiltros("{fechaInicio=2026-11-01}");
        reporte.setFechaGeneracion(LocalDateTime.of(2026, 11, 22, 10, 0));
        return reporte;
    }
}
