package com.congreso.controlador;

import com.congreso.servicio.RecuperacionServicio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class RecuperacionController {
    private static final String MENSAJE_SOLICITUD =
            "Si el correo está registrado, recibirás instrucciones para restablecer la contraseña.";

    private final RecuperacionServicio recuperacionServicio;

    public RecuperacionController(RecuperacionServicio recuperacionServicio) {
        this.recuperacionServicio = recuperacionServicio;
    }

    @GetMapping("/recuperar")
    public String mostrarSolicitud() {
        return "recuperar";
    }

    @PostMapping("/recuperar")
    public String solicitar(@RequestParam(required = false) String correo,
                            RedirectAttributes redirectAttributes) {
        recuperacionServicio.solicitar(correo);
        redirectAttributes.addFlashAttribute("mensaje", MENSAJE_SOLICITUD);
        return "redirect:/recuperar";
    }

    @GetMapping("/recuperar/{token}")
    public String mostrarFormulario(@PathVariable UUID token, Model model) {
        model.addAttribute("token", token);
        return "restablecer-contrasena";
    }

    @PostMapping("/recuperar/{token}")
    public String restablecer(@PathVariable UUID token, @RequestParam String contrasena,
                              RedirectAttributes redirectAttributes) {
        if (recuperacionServicio.restablecer(token, contrasena)) {
            redirectAttributes.addFlashAttribute("mensaje",
                    "Contraseña restablecida. Ya puedes iniciar sesión.");
            return "redirect:/usuarios/login";
        }
        redirectAttributes.addFlashAttribute("error",
                "El enlace no es válido, ya fue usado o expiró. Solicita uno nuevo.");
        return "redirect:/recuperar";
    }
}
