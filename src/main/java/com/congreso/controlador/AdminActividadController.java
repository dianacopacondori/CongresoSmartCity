package com.congreso.controlador;

import com.congreso.dto.ActividadFormulario;
import com.congreso.modelo.Actividad;
import com.congreso.servicio.ActividadServicio;
import com.congreso.servicio.ColaboradorServicio;
import com.congreso.servicio.SalaServicio;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/actividades")
public class AdminActividadController {

    private final ActividadServicio actividadServicio;
    private final SalaServicio salaServicio;
    private final ColaboradorServicio colaboradorServicio;

    public AdminActividadController(ActividadServicio actividadServicio, SalaServicio salaServicio,
                                    ColaboradorServicio colaboradorServicio) {
        this.actividadServicio = actividadServicio;
        this.salaServicio = salaServicio;
        this.colaboradorServicio = colaboradorServicio;
    }

    @InitBinder("form")
    public void protegerIdentificador(WebDataBinder binder) {
        binder.setDisallowedFields("codigo");
    }

    @GetMapping
    public String listar(Model model, HttpSession session) {
        model.addAttribute("usuario", session.getAttribute("usuario"));
        model.addAttribute("actividades", actividadServicio.listarTodasLasActividades());
        return "admin/admin-actividades";
    }

    @GetMapping("/nueva")
    public String nueva(Model model, HttpSession session) {
        model.addAttribute("form", new ActividadFormulario());
        prepararFormulario(model, session, false);
        return "admin/actividad-form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") ActividadFormulario form, BindingResult errores,
                        Model model, HttpSession session, RedirectAttributes redirect) {
        if (errores.hasErrors()) return mostrarError(model, session, false, "Revisa los datos de la actividad");
        try {
            actividadServicio.crearDesdeFormulario(form);
            redirect.addFlashAttribute("mensaje", "Actividad creada exitosamente");
            return "redirect:/admin/actividades";
        } catch (IllegalArgumentException ex) {
            return mostrarError(model, session, false, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            return mostrarError(model, session, false,
                    "No se pudo guardar la actividad. Verifica la sala, el responsable y que el horario esté libre.");
        }
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, HttpSession session) {
        Actividad actividad = actividadServicio.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        model.addAttribute("form", ActividadFormulario.desde(actividad));
        prepararFormulario(model, session, true);
        return "admin/actividad-form";
    }

    @PostMapping("/editar/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ActividadFormulario form,
                             BindingResult errores, Model model, HttpSession session, RedirectAttributes redirect) {
        form.setCodigo(id);
        if (errores.hasErrors()) return mostrarError(model, session, true, "Revisa los datos de la actividad");
        if (!actividadServicio.estaDentroDeVentanaDeCambios()) {
            return mostrarError(model, session, true, "La ventana para modificar actividades está cerrada");
        }
        try {
            actividadServicio.actualizarDesdeFormulario(id, form);
            redirect.addFlashAttribute("mensaje", "Actividad actualizada exitosamente");
            return "redirect:/admin/actividades";
        } catch (IllegalArgumentException ex) {
            return mostrarError(model, session, true, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            return mostrarError(model, session, true,
                    "No se pudo guardar la actividad. Verifica la sala, el responsable y que el horario esté libre.");
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        if (!actividadServicio.estaDentroDeVentanaDeCambios()) {
            redirect.addFlashAttribute("error", "La ventana para cancelar actividades está cerrada");
            return "redirect:/admin/actividades";
        }
        try {
            actividadServicio.cancelarActividad(id);
            redirect.addFlashAttribute("mensaje", "Actividad cancelada exitosamente");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/actividades";
    }

    private String mostrarError(Model model, HttpSession session, boolean edicion, String mensaje) {
        model.addAttribute("error", mensaje);
        prepararFormulario(model, session, edicion);
        return "admin/actividad-form";
    }

    private void prepararFormulario(Model model, HttpSession session, boolean edicion) {
        model.addAttribute("edicion", edicion);
        model.addAttribute("usuario", session.getAttribute("usuario"));
        model.addAttribute("salas", salaServicio.obtenerTodasLasSalas());
        model.addAttribute("responsables", java.util.stream.Stream.concat(
                colaboradorServicio.listarPonentes().stream(),
                colaboradorServicio.listarInstructores().stream()).toList());
    }
}
