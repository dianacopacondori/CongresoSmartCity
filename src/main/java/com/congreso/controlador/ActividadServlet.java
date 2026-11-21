package com.congreso.controlador;

import com.congreso.modelo.Actividad;
import com.congreso.modelo.Inscripcion;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.InscripcionServicio;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final InscripcionServicio inscripcionServicio;

    public ActividadServlet(ActividadServicio actividadServicio, InscripcionServicio inscripcionServicio) {
        this.actividadServicio = actividadServicio;
        this.inscripcionServicio = inscripcionServicio;
    }

    /**
     * Lista todas las actividades disponibles para el asistente.
     */
    @GetMapping
    public String listarActividades(Model model, Authentication authentication) {
        List<Actividad> actividades = actividadServicio.obtenerActividadesActivas();
        model.addAttribute("actividades", actividades);
        boolean asistente = authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(rol -> "ROLE_ASISTENTE".equals(rol.getAuthority()));
        Optional<Inscripcion> inscripcion = asistente
                ? inscripcionServicio.inscripcionDisponible(authentication.getName()) : Optional.empty();
        model.addAttribute("puedeInscribir", inscripcion.isPresent());
        Set<Long> seleccionadas = inscripcion.map(i -> i.getActividades().stream()
                .map(Actividad::getCodigo).collect(Collectors.toSet())).orElseGet(Set::of);
        model.addAttribute("actividadesSeleccionadas", seleccionadas);
        model.addAttribute("esAsistente", asistente);
        return "actividades";
    }

    /**
     * Inscribir un asistente en una actividad.
     */
    @PostMapping("/inscribir/{actividadId}")
    public String inscribirEnActividad(@PathVariable Long actividadId,
                                        Principal principal,
                                        RedirectAttributes redirectAttributes) {
        try {
            inscripcionServicio.inscribirActividadPropia(principal.getName(), actividadId);
            redirectAttributes.addFlashAttribute("mensaje", "Actividad agregada a tu agenda confirmada");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/actividades";
    }
}
