package com.congreso.controlador;

import com.congreso.modelo.Actividad;
import com.congreso.modelo.Sala;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.SalaServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final SalaServicio salaServicio;

    @Autowired
    public ActividadServlet(ActividadServicio actividadServicio, SalaServicio salaServicio) {
        this.actividadServicio = actividadServicio;
        this.salaServicio = salaServicio;
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
     * Muestra el formulario para crear una nueva actividad (admin).
     */
    @GetMapping("/nueva")
    public String mostrarFormularioNuevaActividad(Model model) {
        model.addAttribute("actividad", new Actividad() {});
        model.addAttribute("salas", salaServicio.obtenerTodasLasSalas());
        model.addAttribute("tiposActividad", List.of("PONENCIA", "TALLER"));
        return "admin/actividad-formulario";
    }

    /**
     * Procesa la creación de una nueva actividad.
     */
    @PostMapping("/nueva")
    public String crearActividad(@ModelAttribute Actividad actividad,
                                 @RequestParam Long salaId,
                                 RedirectAttributes redirectAttributes) {
        try {
            Sala sala = salaServicio.buscarPorId(salaId)
                    .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada"));
            actividad.setSala(sala);
            actividadServicio.crearActividad(actividad);
            redirectAttributes.addFlashAttribute("mensaje", "Actividad creada exitosamente");
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/actividades/nueva";
        }
    }

    /**
     * Muestra el formulario para editar una actividad existente.
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Actividad actividad = actividadServicio.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        model.addAttribute("actividad", actividad);
        model.addAttribute("salas", salaServicio.obtenerTodasLasSalas());
        return "admin/actividad-formulario";
    }

    /**
     * Procesa la actualización de una actividad.
     */
    @PostMapping("/editar/{id}")
    public String actualizarActividad(@PathVariable Long id,
                                      @ModelAttribute Actividad actividad,
                                      @RequestParam Long salaId,
                                      RedirectAttributes redirectAttributes) {
        try {
            Sala sala = salaServicio.buscarPorId(salaId)
                    .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada"));
            actividad.setSala(sala);
            actividad.setCodigo(id);
            actividadServicio.actualizarActividad(actividad);
            redirectAttributes.addFlashAttribute("mensaje", "Actividad actualizada exitosamente");
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/actividades/editar/" + id;
        }
    }

    /**
     * Elimina una actividad (cambio de estado a CANCELADA).
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarActividad(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            actividadServicio.cancelarActividad(id);
            redirectAttributes.addFlashAttribute("mensaje", "Actividad cancelada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
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
