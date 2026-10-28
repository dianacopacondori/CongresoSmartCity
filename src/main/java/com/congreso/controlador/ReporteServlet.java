package com.congreso.controlador;

import com.congreso.modelo.Reporte;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.ReporteServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador para la generación y descarga de reportes.
 * Restringe el acceso exclusivamente a usuarios con rol de ADMINISTRADOR.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Controller
@RequestMapping("/reportes")
public class ReporteServlet {

    private final ReporteServicio reporteServicio;

    @Autowired
    public ReporteServlet(ReporteServicio reporteServicio) {
        this.reporteServicio = reporteServicio;
    }

    /**
     * Verifica si el usuario en sesión es un administrador.
     */
    private boolean esAdministrador(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        return usuario != null && usuario.getRol() == RolUsuario.ADMINISTRADOR;
    }

    /**
     * Muestra el menú de reportes disponibles para el administrador.
     */
    @GetMapping("/admin/menu")
    public String mostrarMenuReportes(HttpSession session, Model model) {
        if (!esAdministrador(session)) {
            return "redirect:/usuarios/login";
        }

        model.addAttribute("tiposReporte", List.of(
                "ASISTENTES", "PONENTES", "COLABORADORES",
                "PAGOS", "ACTIVIDADES", "ASISTENCIAS"
        ));
        return "admin/reportes-menu";
    }

    /**
     * Genera un reporte específico y muestra los resultados en pantalla.
     */
    @GetMapping("/admin/generar")
    public String generarReporte(
            @RequestParam String tipo,
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            @RequestParam(required = false) Long actividadId,
            HttpSession session,
            Model model) {

        if (!esAdministrador(session)) {
            return "redirect:/usuarios/login";
        }

        try {
            Reporte reporte = reporteServicio.generarReporte(tipo, fechaInicio, fechaFin, actividadId);
            model.addAttribute("reporte", reporte);
            return "admin/reporte-resultado";
        } catch (Exception e) {
            model.addAttribute("error", "Error al generar el reporte: " + e.getMessage());
            return "admin/reportes-menu";
        }
    }

    /**
     * Exporta un reporte a formato Excel y fuerza la descarga en el navegador.
     * Implementa la directiva Cache-Control: no-store por seguridad.
     */
    @GetMapping("/admin/exportar-excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam String tipo,
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            @RequestParam(required = false) Long actividadId,
            HttpSession session) {

        if (!esAdministrador(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // 1. Generar el reporte con los filtros
            Reporte reporte = reporteServicio.generarReporte(tipo, fechaInicio, fechaFin, actividadId);

            // 2. Convertir el reporte a bytes de Excel usando Apache POI (en el servicio)
            byte[] excelBytes = reporteServicio.exportarReporteExcel(reporte);

            // 3. Configurar las cabeceras HTTP para la descarga segura
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            // Nombre del archivo descargable
            String nombreArchivo = "reporte_" + tipo.toLowerCase() + "_" + LocalDate.now() + ".xlsx";
            headers.setContentDispositionFormData("attachment", nombreArchivo);

            // Requisito de seguridad: evitar que el navegador guarde en caché datos sensibles
            headers.setCacheControl("no-store, no-cache, must-revalidate, proxy-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exporta un reporte a formato PDF (Placeholder para futura implementación con iText/Jasper).
     */
    @GetMapping("/admin/exportar-pdf")
    public ResponseEntity<byte[]> exportarPDF(
            @RequestParam String tipo,
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            @RequestParam(required = false) Long actividadId,
            HttpSession session) {

        if (!esAdministrador(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Reporte reporte = reporteServicio.generarReporte(tipo, fechaInicio, fechaFin, actividadId);
            byte[] pdfBytes = reporteServicio.exportarReportePDF(reporte);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_" + tipo.toLowerCase() + ".pdf");
            headers.setCacheControl("no-store");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lista todos los reportes generados previamente (histórico).
     */
    @GetMapping("/admin/listar")
    public String listarReportes(HttpSession session, Model model) {
        if (!esAdministrador(session)) {
            return "redirect:/usuarios/login";
        }

        List<Reporte> reportes = reporteServicio.obtenerTodosLosReportes();
        model.addAttribute("reportes", reportes);
        return "admin/reportes-lista";
    }
}