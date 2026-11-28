package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenciaDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.dao.ReporteDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistencia;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Pago;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Reporte;
import com.congreso.modelo.Taller;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.modelo.enums.TipoReporte;
import com.congreso.util.GeneradorPdfSimple;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ReporteServicio {

    private static final int LINEAS_POR_PAGINA = 52;

    private final AsistenciaDAO asistenciaDAO;
    private final PagoDAO pagoDAO;
    private final UsuarioDAO usuarioDAO;
    private final ActividadDAO actividadDAO;
    private final InscripcionDAO inscripcionDAO;
    private final ReporteDAO reporteDAO;

    public ReporteServicio(AsistenciaDAO asistenciaDAO, PagoDAO pagoDAO, UsuarioDAO usuarioDAO,
                           ActividadDAO actividadDAO, InscripcionDAO inscripcionDAO, ReporteDAO reporteDAO) {
        this.asistenciaDAO = asistenciaDAO;
        this.pagoDAO = pagoDAO;
        this.usuarioDAO = usuarioDAO;
        this.actividadDAO = actividadDAO;
        this.inscripcionDAO = inscripcionDAO;
        this.reporteDAO = reporteDAO;
    }

    public Reporte generarReporte(String tipo, LocalDate fechaInicio, LocalDate fechaFin, Long actividadId) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar un tipo de reporte");
        }
        TipoReporte tipoReporte;
        try {
            tipoReporte = TipoReporte.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de reporte no válido");
        }
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        if (actividadId != null && actividadDAO.findById(actividadId).isEmpty()) {
            throw new IllegalArgumentException("La actividad indicada no existe");
        }

        Map<String, String> mapaFiltros = new LinkedHashMap<>();
        if (fechaInicio != null) mapaFiltros.put("fechaInicio", fechaInicio.toString());
        if (fechaFin != null) mapaFiltros.put("fechaFin", fechaFin.toString());
        if (actividadId != null) mapaFiltros.put("actividadId", actividadId.toString());

        Reporte reporte = new Reporte();
        reporte.setTipo(tipoReporte);
        reporte.setFechaGeneracion(LocalDateTime.now());
        reporte.setFiltros(mapaFiltros.toString());
        reporte.setPeriodo(fechaInicio == null && fechaFin == null
                ? "Sin filtro de fechas" : fechaInicio + " - " + fechaFin);

        Set<Long> usuariosDeActividad = actividadId == null
                ? Set.of() : usuariosVinculadosAActividad(actividadId);

        List<String> resultados = generarResultados(tipoReporte).stream()
                .filter(dato -> cumpleFechas(dato, fechaInicio, fechaFin))
                .filter(dato -> actividadId == null || cumpleActividad(dato, actividadId, usuariosDeActividad))
                .map(this::filaReporte)
                .toList();

        reporte.setResultados(resultados);
        return reporte;
    }

    private List<Object> generarResultados(TipoReporte tipo) {
        return switch (tipo) {
            case ASISTENTES -> new ArrayList<>(usuarioDAO.findByRol(RolUsuario.ASISTENTE));
            case PONENTES -> new ArrayList<>(usuarioDAO.findByRol(RolUsuario.PONENTE));
            case COLABORADORES -> new ArrayList<>(usuarioDAO.findByRol(RolUsuario.COLABORADOR));
            case PAGOS -> new ArrayList<>(pagoDAO.findAll());
            case ACTIVIDADES -> new ArrayList<>(actividadDAO.findAll());
            case ASISTENCIAS -> new ArrayList<>(asistenciaDAO.findAll());
        };
    }

    /** Convierte cada entidad en una fila CSV con columnas estables y legibles. */
    private String filaReporte(Object dato) {
        if (dato instanceof Pago pago) {
            return unir(csv(pago.getCodigoPago()), csv(pago.getFechaPago()), csv(pago.getImporte()),
                    csv(pago.getSaldo()), csv(pago.getEstado()), csv(pago.getModalidad()),
                    csv(pago.getInscripcion() == null ? null : pago.getInscripcion().getCodigo()),
                    csv(pago.getAsistente() == null ? null : pago.getAsistente().getCorreo()));
        }
        if (dato instanceof Asistencia asistencia) {
            return unir(csv(asistencia.getCodigo()), csv(asistencia.getFecha()), csv(asistencia.getHora()),
                    csv(asistencia.getMecanismoValidacion()), csv(asistencia.getEstado()),
                    csv(asistencia.getActividad() == null ? null : asistencia.getActividad().getCodigo()),
                    csv(asistencia.getActividad() == null ? null : asistencia.getActividad().getTitulo()),
                    csv(asistencia.getAsistente() == null ? null : asistencia.getAsistente().getCorreo()));
        }
        if (dato instanceof Actividad actividad) {
            return unir(csv(actividad.getCodigo()), csv(actividad.getTitulo()), csv(actividad.getFecha()),
                    csv(actividad.getHorarioInicio()), csv(actividad.getHorarioFin()),
                    csv(actividad.getInscritos()), csv(actividad.getCupo()), csv(actividad.getEstado()),
                    csv(actividad.getSala() == null ? null : actividad.getSala().getNombre()));
        }
        if (dato instanceof Usuario usuario) {
            return unir(csv(usuario.getCodigo()), csv(usuario.getNombres()), csv(usuario.getApellidos()),
                    csv(usuario.getCorreo()), csv(usuario.getRol()), csv(usuario.getEstado()),
                    csv(usuario.getFechaCreacion()));
        }
        return csv(dato);
    }

    private String unir(String... columnas) {
        return String.join(",", columnas);
    }

    private String csv(Object valor) {
        String texto = valor == null ? "" : String.valueOf(valor);
        if (texto.indexOf(',') >= 0 || texto.indexOf('"') >= 0 || texto.indexOf('\n') >= 0) {
            return '"' + texto.replace("\"", "\"\"") + '"';
        }
        return texto;
    }

    /** Parser CSV inverso, usado para escribir cada columna en su celda de Excel. */
    private List<String> dividirCsv(String fila) {
        List<String> columnas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < fila.length(); i++) {
            char caracter = fila.charAt(i);
            if (caracter == '"') {
                if (entreComillas && i + 1 < fila.length() && fila.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (caracter == ',' && !entreComillas) {
                columnas.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(caracter);
            }
        }
        columnas.add(actual.toString());
        return columnas;
    }

    private boolean cumpleFechas(Object dato, LocalDate inicio, LocalDate fin) {
        if (inicio == null && fin == null) return true;
        LocalDate fecha = fechaDe(dato);
        if (fecha == null) return false;
        return (inicio == null || !fecha.isBefore(inicio)) && (fin == null || !fecha.isAfter(fin));
    }

    private LocalDate fechaDe(Object dato) {
        if (dato instanceof Usuario usuario) {
            return usuario.getFechaCreacion() == null ? null : usuario.getFechaCreacion().toLocalDate();
        }
        if (dato instanceof Pago pago) return pago.getFechaPago();
        if (dato instanceof Actividad actividad) return actividad.getFecha();
        if (dato instanceof Asistencia asistencia) return asistencia.getFecha();
        return null;
    }

    private boolean cumpleActividad(Object dato, Long actividadId, Set<Long> usuariosDeActividad) {
        if (dato instanceof Actividad actividad) {
            return actividadId.equals(actividad.getCodigo());
        }
        if (dato instanceof Asistencia asistencia) {
            return asistencia.getActividad() != null && actividadId.equals(asistencia.getActividad().getCodigo());
        }
        if (dato instanceof Pago pago) {
            return pago.getInscripcion() != null && pago.getInscripcion().getActividades().stream()
                    .anyMatch(a -> actividadId.equals(a.getCodigo()));
        }
        if (dato instanceof Usuario usuario) {
            return usuariosDeActividad.contains(usuario.getCodigo());
        }
        return false;
    }

    private Set<Long> usuariosVinculadosAActividad(Long actividadId) {
        Set<Long> codigos = new HashSet<>();
        Actividad actividad = actividadDAO.findById(actividadId).orElseThrow();
        if (actividad instanceof Ponencia ponencia && ponencia.getPonente() != null) {
            codigos.add(ponencia.getPonente().getCodigo());
        }
        if (actividad instanceof Taller taller && taller.getInstructor() != null) {
            codigos.add(taller.getInstructor().getCodigo());
        }
        inscripcionDAO.findAll().stream()
                .filter(i -> i.getActividades().stream().anyMatch(a -> actividadId.equals(a.getCodigo())))
                .filter(i -> i.getAsistente() != null)
                .map(i -> i.getAsistente().getCodigo())
                .forEach(codigos::add);
        return codigos;
    }

    public Reporte guardarReporte(Reporte reporte) {
        return reporteDAO.saveAndFlush(reporte);
    }

    public List<Reporte> obtenerTodosLosReportes() {
        return reporteDAO.findAllByOrderByFechaGeneracionDesc();
    }

    public byte[] exportarReporteExcel(Reporte reporte) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet hoja = workbook.createSheet("Reporte");
            int fila = 0;
            fila = escribirFila(hoja, fila, "Reporte " + reporte.getTipo());
            fila = escribirFila(hoja, fila, "Filtros: " + (reporte.getFiltros() == null ? "ninguno" : reporte.getFiltros()));
            fila = escribirFila(hoja, fila, "Generado: " + reporte.getFechaGeneracion());
            fila = escribirFila(hoja, fila, "Resultados");
            if (reporte.getResultados() == null || reporte.getResultados().isEmpty()) {
                escribirFila(hoja, fila, "No se encontraron resultados.");
            } else {
                for (String resultado : reporte.getResultados()) {
                    fila = escribirFilaCsv(hoja, fila, resultado);
                }
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        }
    }

    private int escribirFila(Sheet hoja, int indice, String valor) {
        Row fila = hoja.createRow(indice);
        Cell celda = fila.createCell(0);
        celda.setCellValue(valor == null ? "" : valor);
        return indice + 1;
    }

    /** Cada coma de la fila CSV se escribe en una celda distinta. */
    private int escribirFilaCsv(Sheet hoja, int indice, String filaCsv) {
        Row fila = hoja.createRow(indice);
        List<String> columnas = dividirCsv(filaCsv == null ? "" : filaCsv);
        for (int i = 0; i < columnas.size(); i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(columnas.get(i));
        }
        return indice + 1;
    }

    /**
     * Genera un PDF válido sin añadir dependencias: una página por bloque de líneas,
     * fuente Helvetica estándar y codificación Latin-1.
     */
    public byte[] exportarReportePDF(Reporte reporte) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Reporte: " + reporte.getTipo());
        lineas.add("Generado: " + reporte.getFechaGeneracion());
        lineas.add("Filtros: " + (reporte.getFiltros() == null ? "ninguno" : reporte.getFiltros()));
        lineas.add("");
        if (reporte.getResultados() == null || reporte.getResultados().isEmpty()) {
            lineas.add("No se encontraron resultados.");
        } else {
            lineas.addAll(reporte.getResultados());
        }

        List<String> lineasAjustadas = new ArrayList<>();
        for (String linea : lineas) {
            lineasAjustadas.addAll(ajustarLinea(linea, 90));
        }
        return GeneradorPdfSimple.generar(lineasAjustadas, LINEAS_POR_PAGINA);
    }

    /** Divide líneas largas en palabras para que no se salgan del ancho de la página. */
    private List<String> ajustarLinea(String linea, int ancho) {
        String texto = linea == null ? "" : linea;
        if (texto.length() <= ancho) return List.of(texto);
        List<String> partes = new ArrayList<>();
        String restante = texto;
        while (restante.length() > ancho) {
            int corte = restante.lastIndexOf(' ', ancho);
            if (corte <= 0) corte = ancho;
            partes.add(restante.substring(0, corte));
            restante = restante.substring(corte).trim();
        }
        if (!restante.isEmpty()) partes.add(restante);
        return partes;
    }
}
