package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenciaDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.*;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.modelo.enums.TipoReporte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ReporteServicio {

    private final AsistenciaDAO asistenciaDAO;
    private final PagoDAO pagoDAO;
    private final UsuarioDAO usuarioDAO;
    private final ActividadDAO actividadDAO;

    @Autowired
    public ReporteServicio(AsistenciaDAO asistenciaDAO,
                           PagoDAO pagoDAO,
                           UsuarioDAO usuarioDAO,
                           ActividadDAO actividadDAO) {
        this.asistenciaDAO = asistenciaDAO;
        this.pagoDAO = pagoDAO;
        this.usuarioDAO = usuarioDAO;
        this.actividadDAO = actividadDAO;
    }

    public Reporte generarReporte(String tipo, LocalDate fechaInicio, LocalDate fechaFin, Long actividadId) {
        Reporte reporte = new Reporte();
        reporte.setTipo(TipoReporte.valueOf(tipo.toUpperCase()));
        reporte.setFechaGeneracion(LocalDateTime.now());

        // 1. Creamos el mapa con los filtros
        Map<String, String> mapaFiltros = new HashMap<>();
        if (fechaInicio != null) mapaFiltros.put("fechaInicio", fechaInicio.toString());
        if (fechaFin != null) mapaFiltros.put("fechaFin", fechaFin.toString());
        if (actividadId != null) mapaFiltros.put("actividadId", actividadId.toString());

        // 2. Convertimos el Mapa a String y se lo pasamos a la entidad
        // .toString() convierte {fechaInicio=2026-10-28} en un texto legible para la columna TEXT de la BD
        reporte.setFiltros(mapaFiltros.toString());

        // 3. Generamos los resultados y los convertimos a List<String>
        List<Object> resultadosObj = generarResultados(tipo, fechaInicio, fechaFin, actividadId);
        List<String> resultadosStr = resultadosObj.stream()
                .map(obj -> obj.toString())
                .toList();

        // 4. Asignamos la lista de Strings
        reporte.setResultados(resultadosStr);

        return reporte;
    }

    private List<Object> generarResultados(String tipo, LocalDate fechaInicio, LocalDate fechaFin, Long actividadId) {
        switch (tipo.toUpperCase()) {
            case "ASISTENTES":
                return new ArrayList<>(usuarioDAO.findByRol(RolUsuario.ASISTENTE));
            case "PONENTES":
                return new ArrayList<>(usuarioDAO.findByRol(RolUsuario.PONENTE));
            case "COLABORADORES":
                return new ArrayList<>(usuarioDAO.findByRol(RolUsuario.COLABORADOR));
            case "PAGOS":
                return new ArrayList<>(pagoDAO.findAll());
            case "ACTIVIDADES":
                return new ArrayList<>(actividadDAO.findAll());
            case "ASISTENCIAS":
                return new ArrayList<>(asistenciaDAO.findAll());
            default:
                throw new IllegalArgumentException("Tipo de reporte no válido: " + tipo);
        }
    }

    // Métodos placeholder para exportación (puedes implementarlos con Apache POI luego)
    public byte[] exportarReporteExcel(Reporte reporte) {
        return new byte[0];
    }

    public byte[] exportarReportePDF(Reporte reporte) {
        return new byte[0];
    }

    public List<Reporte> obtenerTodosLosReportes() {
        return Collections.emptyList();
    }
}