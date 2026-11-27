package com.congreso.controlador;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.UsuarioServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;

/**
 * Controlador para la gestión de usuarios.
 * Maneja las peticiones HTTP relacionadas con registro, login y gestión de usuarios.
 *
 * @author cvdiana
 * @version 1.0.0
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioServlet {

    private final UsuarioServicio usuarioServicio;
    @Autowired
    public UsuarioServlet(UsuarioServicio usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    @InitBinder("usuario")
    public void configurarRegistro(WebDataBinder binder) {
        binder.setAllowedFields("nombres", "apellidos", "identificacion", "edad", "correo",
                "telefono", "contrasena", "procedencia", "ocupacion", "universidad",
                "empresa", "modalidadPagoPreferida");
    }

    /**
     * Muestra la página de registro.
     *
     * @param model el modelo para la vista
     * @return el nombre de la vista de registro
     */
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Asistente());
        return "registro";
    }

    /**
     * Procesa el registro de un nuevo usuario.
     *
     * @param usuario el usuario a registrar
     * @param redirectAttributes atributos para redirección
     * @return redirección a la página de login
     */
    @PostMapping("/registro")
    public String procesarRegistro(@Valid @ModelAttribute("usuario") Asistente usuario,
                                   BindingResult errores,
                                   RedirectAttributes redirectAttributes) {
        if (errores.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Revisa los datos del formulario");
            return "redirect:/usuarios/registro";
        }
        try {
            usuario.setCodigo(null);
            usuario.setRol(RolUsuario.ASISTENTE);
            usuarioServicio.registrarUsuario(usuario);
            redirectAttributes.addFlashAttribute("mensaje", "Registro exitoso. Por favor inicie sesión.");
            return "redirect:/usuarios/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios/registro";
        }
    }

    /**
     * Muestra la página de login.
     *
     * @return el nombre de la vista de login
     */
    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(required = false) String error,
                               @RequestParam(required = false) String logout, Model model) {
        if (error != null) model.addAttribute("error", "Credenciales incorrectas o cuenta suspendida");
        if (logout != null) model.addAttribute("mensaje", "Sesión cerrada correctamente");
        return "login";
    }

    /**
     * Muestra el dashboard del usuario.
     *
     * @param session la sesión HTTP
     * @param model el modelo para la vista
     * @return el nombre de la vista del dashboard
     */
    @GetMapping("/dashboard")
    public String mostrarDashboard(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/usuarios/login";
        }

        return usuario.getRol() == RolUsuario.ADMINISTRADOR
                ? "redirect:/admin/dashboard" : usuario.getRol() == RolUsuario.ASISTENTE
                ? "redirect:/dashboard-asistente" : "redirect:/";
    }
}
