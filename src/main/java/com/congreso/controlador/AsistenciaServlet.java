package com.congreso.controlador;

import com.congreso.modelo.Asistencia;
import com.congreso.modelo.Usuario;
import java.security.Principal;
import com.congreso.servicio.AsistenciaServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador para el registro y consulta de asistencias.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Controller
@RequestMapping("/asistencias")
public class AsistenciaServlet {

    private final AsistenciaServicio asistenciaServicio;

    @Autowired
    public AsistenciaServlet(AsistenciaServicio asistenciaServicio) {
        this.asistenciaServicio = asistenciaServicio;
    }

    /**
     * Muestra el formulario para registrar asistencia (personal de recepción).
     */
    @GetMapping("/registrar")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("asistencia", new Asistencia());
        return "asistencia-registro";
    }

    /**
     * Procesa el registro de asistencia.
     * Se ajustó para coincidir con la firma del servicio: (codigoAsistente, codigoActividad, mecanismo)
     */
    @PostMapping("/registrar")
    public String registrarAsistencia(@RequestParam Long codigoAsistente,
                                      @RequestParam Long codigoActividad,
                                      @RequestParam String mecanismoValidacion,
                                      @RequestParam(required = false) Long codigoFotocheck,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            asistenciaServicio.registrarAsistencia(codigoAsistente, codigoActividad,
                    mecanismoValidacion, codigoFotocheck, principal == null ? null : principal.getName());
            redirectAttributes.addFlashAttribute("mensaje", "Asistencia registrada y verificada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistencias/registrar";
    }

    /**
     * Consulta el récord de asistencias del asistente logueado.
     */
    @GetMapping("/mis-asistencias")
    public String consultarMisAsistencias(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/login";
        }

        List<Asistencia> asistencias = asistenciaServicio.listarAsistenciasPorAsistente(usuario.getCodigo());
        model.addAttribute("asistencias", asistencias);
        return "mis-asistencias";
    }

    /**
     * Lista todas las asistencias para el administrador.
     */
    @GetMapping("/admin/listar")
    public String listarAsistenciasAdmin(@RequestParam(required = false) Long actividadId,
                                         Model model) {
        List<Asistencia> asistencias;
        if (actividadId != null) {
            asistencias = asistenciaServicio.consultarAsistenciasPorActividad(actividadId);
        } else {
            asistencias = asistenciaServicio.obtenerTodasLasAsistencias();
        }
        model.addAttribute("asistencias", asistencias);
        return "admin/asistencias-lista";
    }

    /**
     * Corrige un registro de asistencia (solo administrador).
     */
    @PostMapping("/admin/corregir/{id}")
    public String corregirAsistencia(@PathVariable Long id,
                                     @RequestParam String justificacion,
                                     HttpSession session,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                return "redirect:/usuarios/login";
            }

            asistenciaServicio.corregirAsistencia(id, justificacion,
                    principal == null ? null : principal.getName());
            redirectAttributes.addFlashAttribute("mensaje", "Asistencia corregida exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistencias/admin/listar";
    }
}