package com.congreso.controlador;

import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.servicio.PagoServicio;
import com.congreso.dto.EstadoParticipacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.security.Principal;

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
    public String mostrarFormularioPago(Principal principal, Model model) {
        model.addAttribute("inscripciones", pagoServicio.inscripcionesPendientesPropias(principal.getName()));
        model.addAttribute("pasoSiguiente", pagoServicio.estadoParticipacion(principal.getName()).paso().name());
        return "pago-formulario";
    }

    @PostMapping("/nuevo")
    public String registrarPago(@RequestParam Long codigoInscripcion,
                                @RequestParam ModalidadPago modalidad,
                                Principal principal, RedirectAttributes redirectAttributes) {
        try {
            Pago pago = pagoServicio.simularPago(principal.getName(), codigoInscripcion, modalidad);
            redirectAttributes.addFlashAttribute("mensaje", pago.estaPagado()
                    ? "Pago simulado completado. Inscripción confirmada automáticamente (sin cobro real)."
                    : "Primera cuota simulada registrada. Inscripción pendiente hasta completar la segunda cuota (sin cobro real).");
            return "redirect:/pagos/confirmacion";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pagos/nuevo";
        }
    }

    @PostMapping("/abonar/{pagoId}")
    public String simularSegundaCuota(@PathVariable Long pagoId, Principal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            pagoServicio.simularSegundaCuota(principal.getName(), pagoId);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Segunda cuota simulada registrada. Inscripción confirmada automáticamente (sin cobro real).");
            return "redirect:/pagos/confirmacion";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pagos/mis-pagos";
        }
    }

    @GetMapping("/confirmacion")
    public String mostrarConfirmacionPago(Principal principal, Model model) {
        EstadoParticipacion estado = pagoServicio.estadoParticipacion(principal.getName());
        model.addAttribute("pasoSiguiente", estado.paso().name());
        return "pago-confirmacion";
    }

    @GetMapping("/mis-pagos")
    public String consultarMisPagos(Principal principal, Model model) {
        model.addAttribute("pagos", pagoServicio.pagosPropios(principal.getName()));
        return "mis-pagos";
    }

    @GetMapping("/admin/listar")
    public String listarPagos(@RequestParam(required = false) String estado,
                              @RequestParam(required = false) String nombre,
                              Model model) {
        try {
            model.addAttribute("pagos", pagoServicio.filtrarPagos(estado, nombre));
        } catch (IllegalArgumentException e) {
            model.addAttribute("pagos", List.of());
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("estado", estado == null ? "" : estado);
        model.addAttribute("nombre", nombre == null ? "" : nombre);
        return "admin/pagos-lista";
    }

}
