package com.congreso.controlador;

import com.congreso.dto.SalaFormulario;
import com.congreso.modelo.Sala;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/salas")
public class AdminSalaController {

    private final SalaServicio salaServicio;

    public AdminSalaController(SalaServicio salaServicio) {
        this.salaServicio = salaServicio;
    }

    @InitBinder("form")
    public void protegerFormulario(WebDataBinder binder) {
        binder.setAllowedFields("nombre", "capacidad", "ubicacion");
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busqueda,
                         HttpSession session, Model model) {
        model.addAttribute("usuario", session.getAttribute("usuario"));
        model.addAttribute("salas", salaServicio.buscarSalas(busqueda));
        model.addAttribute("busqueda", busqueda);
        return "admin/salas-lista";
    }

    @GetMapping("/nueva")
    public String nueva(HttpSession session, Model model) {
        model.addAttribute("form", new SalaFormulario());
        prepararFormulario(model, session, false, null);
        return "admin/sala-form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") SalaFormulario form, BindingResult errores,
                        HttpSession session, Model model, RedirectAttributes redirect) {
        if (errores.hasErrors()) {
            prepararFormulario(model, session, false, null);
            return "admin/sala-form";
        }
        try {
            salaServicio.crearSala(convertir(form));
            redirect.addFlashAttribute("mensaje", "Sala creada exitosamente");
            return "redirect:/admin/salas";
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar la sala. Revisa sus datos.");
        }
        prepararFormulario(model, session, false, null);
        return "admin/sala-form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, HttpSession session, Model model) {
        Sala sala = salaServicio.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("La sala no existe"));
        model.addAttribute("form", SalaFormulario.desde(sala));
        prepararFormulario(model, session, true, id);
        return "admin/sala-form";
    }

    @PostMapping("/editar/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("form") SalaFormulario form,
                             BindingResult errores, HttpSession session, Model model,
                             RedirectAttributes redirect) {
        if (errores.hasErrors()) {
            prepararFormulario(model, session, true, id);
            return "admin/sala-form";
        }
        try {
            salaServicio.actualizarSala(id, convertir(form));
            redirect.addFlashAttribute("mensaje", "Sala actualizada exitosamente");
            return "redirect:/admin/salas";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar la sala. Revisa sus datos.");
        }
        prepararFormulario(model, session, true, id);
        return "admin/sala-form";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            salaServicio.eliminarSala(id);
            redirect.addFlashAttribute("mensaje", "Sala eliminada exitosamente");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("error", "No se puede eliminar una sala con actividades asignadas");
        }
        return "redirect:/admin/salas";
    }

    private Sala convertir(SalaFormulario form) {
        return new Sala(form.getNombre().trim(), form.getCapacidad(), form.getUbicacion());
    }

    private void prepararFormulario(Model model, HttpSession session, boolean edicion, Long id) {
        model.addAttribute("usuario", session.getAttribute("usuario"));
        model.addAttribute("edicion", edicion);
        model.addAttribute("id", id);
    }
}
