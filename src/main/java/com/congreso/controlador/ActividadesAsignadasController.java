package com.congreso.controlador;

import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.ColaboradorServicio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class ActividadesAsignadasController {

    private final ColaboradorServicio colaboradorServicio;

    public ActividadesAsignadasController(ColaboradorServicio colaboradorServicio) {
        this.colaboradorServicio = colaboradorServicio;
    }

    @GetMapping("/colaborador/actividades")
    public String actividadesColaborador(Principal principal, Model model) {
        return mostrar(principal, model, RolUsuario.COLABORADOR);
    }

    @GetMapping("/instructor/actividades")
    public String actividadesInstructor(Principal principal, Model model) {
        return mostrar(principal, model, RolUsuario.INSTRUCTOR);
    }

    @GetMapping("/ponente/actividades")
    public String actividadesPonente(Principal principal, Model model) {
        return mostrar(principal, model, RolUsuario.PONENTE);
    }

    private String mostrar(Principal principal, Model model, RolUsuario rol) {
        model.addAttribute("actividades",
                colaboradorServicio.listarActividadesAsignadas(principal.getName(), rol));
        model.addAttribute("rol", rol);
        return "colaborador/actividades-asignadas";
    }
}
