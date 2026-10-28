package com.congreso.controlador;

import com.congreso.modelo.Pago;
import com.congreso.servicio.PagoServicio;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador para la gestión de pagos.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Controller
@RequestMapping("/pagos")
public class PagoServlet {

    private final PagoServicio pagoServicio;

    @Autowired
    public PagoServlet(PagoServicio pagoServicio) {
        this.pagoServicio = pagoServicio;
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioPago(Model model) {
        model.addAttribute("pago", new Pago());
        return "pago-formulario";
    }

    @PostMapping("/nuevo")
    public String registrarPago(@ModelAttribute Pago pago,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            // Ahora el servicio tiene el método sobrecargado que acepta el objeto Pago
            pagoServicio.registrarPago(pago);
            redirectAttributes.addFlashAttribute("mensaje", "Pago registrado exitosamente");
            return "redirect:/pagos/confirmacion";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pagos/nuevo";
        }
    }

    @GetMapping("/confirmacion")
    public String mostrarConfirmacionPago(Model model) {
        return "pago-confirmacion";
    }

    @GetMapping("/mis-pagos")
    public String consultarMisPagos(HttpSession session, Model model) {
        // TODO: Implementar lógica para obtener pagos del usuario en sesión
        return "mis-pagos";
    }

    @GetMapping("/admin/listar")
    public String listarPagos(@RequestParam(required = false) String estado,
                              Model model) {
        List<Pago> pagos;
        if (estado != null && !estado.isEmpty()) {
            pagos = pagoServicio.buscarPorEstado(estado);
        } else {
            pagos = pagoServicio.obtenerTodosLosPagos();
        }
        model.addAttribute("pagos", pagos);
        return "admin/pagos-lista";
    }

    @PostMapping("/admin/registrar-cuota/{pagoId}")
    public String registrarCuota(@PathVariable Long pagoId,
                                 @RequestParam double monto,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Llama al alias que internamente ejecuta registrarAbono
            pagoServicio.registrarCuota(pagoId, monto);
            redirectAttributes.addFlashAttribute("mensaje", "Cuota registrada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pagos/admin/listar";
    }
}