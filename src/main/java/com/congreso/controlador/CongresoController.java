package com.congreso.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador principal para las páginas públicas del congreso.
 * Solo maneja rutas que NO están en otros servlets.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Controller
public class CongresoController {

    /**
     * Página de inicio (landing page pública).
     */
    @GetMapping("/")
    public String index(Model model) {
        // TODO: Cargar ponentes reales desde la base de datos
        model.addAttribute("ponentes", new ArrayList<>());
        return "index";
    }

    /**
     * Página de ponentes (ruta exclusiva, no está en ActividadServlet).
     */
    @GetMapping("/ponentes")
    public String ponentes(Model model) {
        // TODO: Cargar ponentes reales desde la base de datos
        model.addAttribute("ponentes", new ArrayList<>());
        return "ponentes";
    }

    /**
     * Página de contacto.
     */
    @GetMapping("/contacto")
    public String contacto() {
        return "contacto";
    }

    /**
     * Dashboard del asistente (ruta exclusiva).
     */
    @GetMapping("/dashboard-asistente")
    public String dashboardAsistente(Model model) {
        // TODO: Cargar datos reales del asistente desde la sesión
        model.addAttribute("actividades", new ArrayList<>());
        model.addAttribute("asistencias", new ArrayList<>());
        return "dashboard-asistente";
    }

    /**
     * Dashboard del administrador (ruta exclusiva).
     */
    @GetMapping("/admin/dashboard")
    public String dashboardAdmin(Model model) {
        // TODO: Cargar estadísticas reales desde la base de datos
        model.addAttribute("totalAsistentes", 1250);
        model.addAttribute("registradosHoy", 85);
        model.addAttribute("totalIngresos", "125,000");
        model.addAttribute("pagosPendientes", 12);
        model.addAttribute("actividades", new ArrayList<>());
        model.addAttribute("pagos", new ArrayList<>());
        return "dashboard-admin";
    }
}