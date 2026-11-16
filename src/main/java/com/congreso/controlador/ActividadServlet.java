package com.congreso.controlador;

import com.congreso.modelo.Actividad;
import com.congreso.servicio.ActividadServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador para la gestión de actividades (ponencias y talleres).
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Controller
@RequestMapping("/actividades")
public class ActividadServlet {

    private final ActividadServicio actividadServicio;

    public ActividadServlet(ActividadServicio actividadServicio) {
        this.actividadServicio = actividadServicio;
    }

    /**
     * Lista todas las actividades disponibles para el asistente.
     */
    @GetMapping
    public String listarActividades(Model model) {
        List<Actividad> actividades = actividadServicio.obtenerActividadesActivas();
        model.addAttribute("actividades", actividades);
        return "actividades";
    }

    /**
     * Inscribir un asistente en una actividad.
     */
    @PostMapping("/inscribir/{actividadId}")
    public String inscribirEnActividad(@PathVariable Long actividadId,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        try {
            // Obtener asistente de la sesión
            // TODO: Implementar lógica de inscripción
            redirectAttributes.addFlashAttribute("mensaje", "Inscripción exitosa");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/actividades";
    }
}
