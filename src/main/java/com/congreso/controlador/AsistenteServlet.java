package com.congreso.controlador;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.servicio.AsistenteServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String mostrarMiPerfil(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/login";
        }

        Asistente asistente = asistenteServicio.buscarPorCodigo(usuario.getCodigo());
        model.addAttribute("asistente", asistente);
        return "asistente-perfil";
    }

    /**
     * Actualiza los datos del asistente logueado.
     */
    @PostMapping("/mi-perfil")
    public String actualizarMiPerfil(@ModelAttribute Asistente asistente,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        try {
            asistenteServicio.actualizarAsistente(asistente);
            redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistentes/mi-perfil";
    }

    /**
     * Suspende un asistente (admin).
     */
    @GetMapping("/admin/suspender/{id}")
    public String suspenderAsistente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            asistenteServicio.suspenderAsistente(id);
            redirectAttributes.addFlashAttribute("mensaje", "Asistente suspendido exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistentes/admin/listar";
    }

    /**
     * Elimina lógicamente un asistente (admin).
     */
    @GetMapping("/admin/eliminar/{id}")
    public String eliminarAsistente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            asistenteServicio.eliminarAsistente(id);
            redirectAttributes.addFlashAttribute("mensaje", "Asistente eliminado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asistentes/admin/listar";
    }
}