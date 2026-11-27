package com.congreso.controlador;

import com.congreso.modelo.Actividad;
import com.congreso.modelo.Inscripcion;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.InscripcionServicio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
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
    public String listarActividades(
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model, Authentication authentication) {
        String nombreFiltro = nombre == null ? "" : nombre.trim();
        String tipoFiltro = tipo == null ? "" : tipo.trim();
        boolean sinFiltros = nombreFiltro.isEmpty() && tipoFiltro.isEmpty() && fecha == null;
        List<Actividad> actividades;
        try {
            actividades = sinFiltros
                    ? actividadServicio.obtenerActividadesActivas()
                    : actividadServicio.obtenerActividadesFiltradas(nombreFiltro, tipoFiltro, fecha);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            actividades = List.of();
        }
        model.addAttribute("actividades", actividades);
        model.addAttribute("nombreFiltro", nombreFiltro);
        model.addAttribute("tipoFiltro", tipoFiltro);
        model.addAttribute("fechaFiltro", fecha);
        model.addAttribute("hayFiltros", !nombreFiltro.isEmpty() || !tipoFiltro.isEmpty() || fecha != null);
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
