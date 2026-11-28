package com.congreso.controlador;

import com.congreso.dto.PerfilAsistenteFormulario;
import com.congreso.modelo.Asistente;
import com.congreso.servicio.AsistenteServicio;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Controlador para la gestión de asistentes.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Controller
@RequestMapping("/asistentes")
public class AsistenteServlet {

    private final AsistenteServicio asistenteServicio;

    @Autowired
    public AsistenteServlet(AsistenteServicio asistenteServicio) {
        this.asistenteServicio = asistenteServicio;
    }

    @InitBinder("perfil")
    public void protegerPerfil(WebDataBinder binder) {
        binder.setAllowedFields("nombres", "apellidos", "telefono", "procedencia",
                "ocupacion", "universidad", "empresa", "edad", "modalidadPagoPreferida");
    }

    /**
     * Lista todos los asistentes (admin).
     */
    @GetMapping("/admin/listar")
    public String listarAsistentes(@RequestParam(required = false) String busqueda,
                                   Model model) {
        List<Asistente> asistentes;
        if (busqueda != null && !busqueda.isEmpty()) {
            asistentes = asistenteServicio.buscarPorNombre(busqueda);
        } else {
            asistentes = asistenteServicio.obtenerTodosLosAsistentes();
        }
        model.addAttribute("asistentes", asistentes);
        return "admin/asistentes-lista";
    }

    /**
     * Muestra el perfil del asistente logueado.
     */
    @GetMapping("/mi-perfil")
    public String mostrarMiPerfil(Principal principal, Model model) {
        Asistente asistente = asistenteServicio.buscarPerfilPropio(principal.getName());
        model.addAttribute("asistente", asistente);
        model.addAttribute("perfil", PerfilAsistenteFormulario.desde(asistente));
        return "asistente-perfil";
    }

    /**
     * Actualiza los datos del asistente logueado.
     */
    @PostMapping("/mi-perfil")
    public String actualizarMiPerfil(@Valid @ModelAttribute("perfil") PerfilAsistenteFormulario perfil,
                                     BindingResult errores, Principal principal, HttpSession session,
                                     Model model, RedirectAttributes redirectAttributes) {
        if (errores.hasErrors()) {
            model.addAttribute("asistente", asistenteServicio.buscarPerfilPropio(principal.getName()));
            return "asistente-perfil";
        }
        try {
            Asistente actualizado = asistenteServicio.actualizarPerfilPropio(principal.getName(), perfil);
            session.setAttribute("usuario", actualizado);
            redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado exitosamente");
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("asistente", asistenteServicio.buscarPerfilPropio(principal.getName()));
            model.addAttribute("error", "No se pudo guardar el perfil. Revisa los datos e inténtalo nuevamente.");
            return "asistente-perfil";
        }
        return "redirect:/asistentes/mi-perfil";
    }

    /**
     * Suspende un asistente (admin).
     */
    @PostMapping("/admin/suspender/{id}")
    public String suspenderAsistente(@PathVariable Long id, Principal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            asistenteServicio.suspenderAsistente(id, principal == null ? null : principal.getName());
            redirectAttributes.addFlashAttribute("mensaje", "Asistente suspendido (baja lógica)");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistentes/admin/listar";
    }

    /**
     * Da de baja lógicamente a un asistente (admin), con POST y CSRF.
     */
    @PostMapping("/admin/eliminar/{id}")
    public String eliminarAsistente(@PathVariable Long id, Principal principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            asistenteServicio.eliminarAsistente(id, principal == null ? null : principal.getName());
            redirectAttributes.addFlashAttribute("mensaje", "Asistente dado de baja (baja lógica)");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistentes/admin/listar";
    }
}
