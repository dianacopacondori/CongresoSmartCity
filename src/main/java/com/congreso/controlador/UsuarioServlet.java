package com.congreso.controlador;

import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.Autenticador;
import com.congreso.servicio.UsuarioServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

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
    private final Autenticador autenticador;

    @Autowired
    public UsuarioServlet(UsuarioServicio usuarioServicio, Autenticador autenticador) {
        this.usuarioServicio = usuarioServicio;
        this.autenticador = autenticador;
    }

    /**
     * Muestra la página de registro.
     *
     * @param model el modelo para la vista
     * @return el nombre de la vista de registro
     */
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario() {
            @Override
            public String getNombres() { return ""; }
            @Override
            public String getApellidos() { return ""; }
            @Override
            public String getCorreo() { return ""; }
            // Implementar otros getters según sea necesario
        });
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
    public String procesarRegistro(@ModelAttribute Usuario usuario,
                                   RedirectAttributes redirectAttributes) {
        try {
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
    public String mostrarLogin() {
        return "login";
    }

    /**
     * Procesa el login de un usuario.
     *
     * @param correo el correo electrónico del usuario
     * @param contrasena la contraseña del usuario
     * @param session la sesión HTTP
     * @param redirectAttributes atributos para redirección
     * @return redirección al dashboard o de vuelta al login
     */
    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
                                @RequestParam String contrasena,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = autenticador.autenticar(correo, contrasena);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            session.setAttribute("usuario", usuario);
            session.setAttribute("rol", usuario.getRol());
            return "redirect:/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("error", "Credenciales incorrectas o cuenta suspendida");
            return "redirect:/usuarios/login";
        }
    }

    /**
     * Procesa el logout de un usuario.
     *
     * @param session la sesión HTTP
     * @return redirección a la página de login
     */
    @GetMapping("/logout")
    public String procesarLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/usuarios/login";
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

        model.addAttribute("usuario", usuario);
        model.addAttribute("rol", usuario.getRol());
        return "dashboard";
    }
}