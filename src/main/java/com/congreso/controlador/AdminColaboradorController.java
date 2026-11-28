package com.congreso.controlador;

import com.congreso.dto.ColaboradorFormulario;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.ColaboradorServicio;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/admin/colaboradores")
public class AdminColaboradorController {

    private static final List<RolUsuario> TIPOS = List.of(
            RolUsuario.COLABORADOR, RolUsuario.INSTRUCTOR, RolUsuario.PONENTE);

    private final ColaboradorServicio colaboradorServicio;
    private final PasswordEncoder passwordEncoder;

    public AdminColaboradorController(ColaboradorServicio colaboradorServicio, PasswordEncoder passwordEncoder) {
        this.colaboradorServicio = colaboradorServicio;
        this.passwordEncoder = passwordEncoder;
    }

    @InitBinder("form")
    public void protegerFormulario(WebDataBinder binder) {
        binder.setAllowedFields("nombres", "apellidos", "identificacion", "correo", "telefono",
                "institucion", "empresa", "tipoColaborador", "estado", "contrasena");
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busqueda,
                         @RequestParam(required = false) RolUsuario tipo,
                         @RequestParam(required = false) EstadoUsuario estado,
                         HttpSession session, Model model) {
        boolean tipoInvalido = tipo == RolUsuario.ADMINISTRADOR || tipo == RolUsuario.ASISTENTE;
        if (tipoInvalido) {
            model.addAttribute("error", "El tipo seleccionado no corresponde a un colaborador");
        }
        model.addAttribute("usuario", session.getAttribute("usuario"));
        model.addAttribute("colaboradores", tipoInvalido
                ? List.of() : colaboradorServicio.buscarColaboradores(busqueda, tipo, estado));
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("tipoSeleccionado", tipo);
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("tipos", TIPOS);
        model.addAttribute("estados", EstadoUsuario.values());
        return "admin/colaboradores-lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(HttpSession session, Model model) {
        model.addAttribute("form", new ColaboradorFormulario());
        prepararFormulario(model, session, false, null);
        return "admin/colaborador-form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") ColaboradorFormulario form,
                        BindingResult errores, HttpSession session, Model model,
                        RedirectAttributes redirect) {
        if (form.getContrasena() == null || form.getContrasena().length() < 8) {
            errores.rejectValue("contrasena", "Size", "La contraseña debe tener al menos 8 caracteres");
        } else if (excedeLimiteBCrypt(form.getContrasena())) {
            errores.rejectValue("contrasena", "Size", "La contraseña no puede exceder 72 bytes");
        }
        if (errores.hasErrors()) {
            prepararFormulario(model, session, false, null);
            return "admin/colaborador-form";
        }
        try {
            Colaborador colaborador = convertir(form, passwordEncoder.encode(form.getContrasena()));
            colaborador.setEstado(EstadoUsuario.ACTIVO);
            colaboradorServicio.registrarColaborador(colaborador);
            redirect.addFlashAttribute("mensaje", "Colaborador registrado exitosamente");
            return "redirect:/admin/colaboradores";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar. Verifica que el correo y la identificación sean únicos.");
        }
        prepararFormulario(model, session, false, null);
        return "admin/colaborador-form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, HttpSession session, Model model) {
        Colaborador colaborador = colaboradorServicio.buscarPorId(id);
        model.addAttribute("form", ColaboradorFormulario.desde(colaborador));
        prepararFormulario(model, session, true, id);
        return "admin/colaborador-form";
    }

    @PostMapping("/editar/{id}")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ColaboradorFormulario form,
                             BindingResult errores, HttpSession session, Model model,
                             RedirectAttributes redirect) {
        if (errores.hasErrors()) {
            prepararFormulario(model, session, true, id);
            return "admin/colaborador-form";
        }
        if (form.getContrasena() != null && !form.getContrasena().isBlank()
                && excedeLimiteBCrypt(form.getContrasena())) {
            errores.rejectValue("contrasena", "Size", "La contraseña no puede exceder 72 bytes");
            prepararFormulario(model, session, true, id);
            return "admin/colaborador-form";
        }
        try {
            String contrasena = form.getContrasena() == null || form.getContrasena().isBlank()
                    ? null : passwordEncoder.encode(form.getContrasena());
            Colaborador datos = convertir(form, contrasena);
            colaboradorServicio.actualizarColaborador(id, datos);
            redirect.addFlashAttribute("mensaje", "Colaborador actualizado exitosamente");
            return "redirect:/admin/colaboradores";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar. Verifica que el correo y la identificación sean únicos.");
        }
        prepararFormulario(model, session, true, id);
        return "admin/colaborador-form";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            colaboradorServicio.darDeBaja(id);
            redirect.addFlashAttribute("mensaje", "Colaborador dado de baja; sus asignaciones se conservaron");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/colaboradores";
    }

    private Colaborador convertir(ColaboradorFormulario form, String contrasena) {
        RolUsuario tipo = RolUsuario.valueOf(form.getTipoColaborador());
        Colaborador colaborador = new Colaborador();
        colaborador.setNombres(form.getNombres().trim());
        colaborador.setApellidos(form.getApellidos().trim());
        colaborador.setIdentificacion(form.getIdentificacion().trim());
        colaborador.setCorreo(form.getCorreo().trim());
        colaborador.setTelefono(form.getTelefono());
        colaborador.setInstitucion(form.getInstitucion());
        colaborador.setEmpresa(form.getEmpresa());
        colaborador.setTipoColaborador(tipo);
        colaborador.setRol(tipo);
        colaborador.setEstado(form.getEstado() == null ? EstadoUsuario.ACTIVO : form.getEstado());
        colaborador.setContrasena(contrasena);
        return colaborador;
    }

    private void prepararFormulario(Model model, HttpSession session, boolean edicion, Long id) {
        model.addAttribute("usuario", session.getAttribute("usuario"));
        model.addAttribute("edicion", edicion);
        model.addAttribute("id", id);
        model.addAttribute("tipos", TIPOS);
        model.addAttribute("estados", EstadoUsuario.values());
    }

    private boolean excedeLimiteBCrypt(String contrasena) {
        return contrasena.getBytes(StandardCharsets.UTF_8).length > 72;
    }
}
