package com.congreso.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class CongresoController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("ponentes", new ArrayList<>());
        return "index";
    }

    @GetMapping("/actividades")
    public String actividades(Model model) {
        model.addAttribute("actividades", new ArrayList<>());
        return "actividades";
    }

    @GetMapping("/dashboard-asistente")
    public String dashboardAsistente(Model model) {
        model.addAttribute("actividades", new ArrayList<>());
        model.addAttribute("asistencias", new ArrayList<>());
        return "dashboard-asistente";
    }

    @GetMapping("/admin/dashboard")
    public String dashboardAdmin(Model model) {
        model.addAttribute("totalAsistentes", 1250);
        model.addAttribute("registradosHoy", 85);
        model.addAttribute("totalIngresos", "125,000");
        model.addAttribute("pagosPendientes", 12);
        model.addAttribute("actividades", new ArrayList<>());
        model.addAttribute("pagos", new ArrayList<>());
        return "dashboard-admin";
    }
}
