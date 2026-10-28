package com.congreso.controlador;

import com.congreso.dao.AsistenteDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Usuario;
import com.congreso.servicio.InscripcionServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador para la gestión de inscripciones al congreso.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Controller
@RequestMapping("/inscripciones")
public class InscripcionServlet {

    private final InscripcionServicio inscripcionServicio;
    private final AsistenteDAO asistenteDAO;

    @Autowired
    public InscripcionServlet(InscripcionServicio inscripcionServicio,
                              AsistenteDAO asistenteDAO) {
        this.inscripcionServicio = inscripcionServicio;
        this.asistenteDAO = asistenteDAO;
    }

    /**
     * Muestra el formulario de inscripción al congreso.
     */
    @GetMapping("/nueva")
    public String mostrarFormularioInscripcion(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/login";
        }
        model.addAttribute("usuario", usuario);
        return "inscripcion-formulario";
    }

    /**
     * Procesa la creación de una nueva inscripción al congreso.
     */
    @PostMapping("/nueva")
    public String crearInscripcion(@RequestParam String tipo,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "Debe iniciar sesión para inscribirse");
                return "redirect:/usuarios/login";
            }

            // Buscar el Asistente correspondiente al usuario de la sesión
            Asistente asistente = asistenteDAO.findById(usuario.getCodigo())
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el asistente"));

            // Llamar al método real del servicio
            Inscripcion inscripcion = inscripcionServicio.crearInscripcion(asistente, tipo);

            redirectAttributes.addFlashAttribute("mensaje",
                    "Inscripción creada exitosamente. Código: " + inscripcion.getCodigo());
            return "redirect:/inscripciones/confirmacion";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/inscripciones/nueva";
        }
    }

    /**
     * Muestra la confirmación de inscripción.
     */
    @GetMapping("/confirmacion")
    public String mostrarConfirmacion(Model model) {
        return "inscripcion-confirmacion";
    }

    /**
     * Muestra la agenda personalizada del asistente logueado.
     */
    @GetMapping("/mi-agenda")
    public String consultarMiAgenda(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/usuarios/login";
        }

        List<Actividad> agenda = inscripcionServicio.obtenerAgendaPersonalizada(usuario.getCodigo());
        model.addAttribute("actividades", agenda);
        return "agenda-personalizada";
    }

    /**
     * Agrega una actividad a la inscripción del asistente.
     * Usa el método real: inscribirActividad(codigoInscripcion, codigoActividad)
     */
    @PostMapping("/agregar-actividad")
    public String agregarActividad(@RequestParam Long codigoInscripcion,
                                   @RequestParam Long codigoActividad,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                redirectAttributes.addFlashAttribute("error", "Debe iniciar sesión");
                return "redirect:/usuarios/login";
            }

            // Llamar al método real del servicio
            inscripcionServicio.inscribirActividad(codigoInscripcion, codigoActividad);
            redirectAttributes.addFlashAttribute("mensaje", "Actividad agregada a tu agenda");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inscripciones/mi-agenda";
    }

    /**
     * Lista todas las inscripciones (admin).
     * Usa findAll() del DAO directamente.
     */
    @GetMapping("/admin/listar")
    public String listarInscripciones(Model model) {
        List<Inscripcion> inscripciones = inscripcionServicio.listarTodasLasInscripciones();
        model.addAttribute("inscripciones", inscripciones);
        return "admin/inscripciones-lista";
    }

    /**
     * Confirma una inscripción (admin o asistente).
     */
    @PostMapping("/confirmar/{codigo}")
    public String confirmarInscripcion(@PathVariable Long codigo,
                                       RedirectAttributes redirectAttributes) {
        try {
            inscripcionServicio.confirmarInscripcion(codigo);
            redirectAttributes.addFlashAttribute("mensaje", "Inscripción confirmada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inscripciones/admin/listar";
    }

    /**
     * Elimina una inscripción (admin).
     */
    @GetMapping("/admin/eliminar/{codigo}")
    public String eliminarInscripcion(@PathVariable Long codigo,
                                      RedirectAttributes redirectAttributes) {
        try {
            inscripcionServicio.eliminarInscripcion(codigo);
            redirectAttributes.addFlashAttribute("mensaje", "Inscripción eliminada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inscripciones/admin/listar";
    }
}