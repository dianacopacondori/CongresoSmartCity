package com.congreso.controlador;

import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.servicio.PagoServicio;
import com.congreso.servicio.NotificacionServicio;
import com.congreso.dao.AsistenteDAO;
import com.congreso.modelo.Notificacion;
import com.congreso.dto.EstadoParticipacion;
import com.congreso.dto.ResumenInscripcion;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoPago;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.security.Principal;
import java.util.Arrays;
import java.util.Locale;

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
    @Autowired(required = false)
    private AsistenteDAO asistenteDAO;
    @Autowired(required = false)
    private NotificacionServicio notificacionServicio;

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
            Notificacion notificacion = notificarConfirmacionSiCorresponde(principal, pago);
            redirectAttributes.addFlashAttribute("mensaje", pago.estaPagado()
                    ? "Pago simulado completado. Inscripción confirmada automáticamente (sin cobro real)."
                    : "Primera cuota simulada registrada. Inscripción pendiente hasta completar la segunda cuota (sin cobro real).");
            if (notificacion != null) {
                redirectAttributes.addFlashAttribute("estadoNotificacion", textoEstadoNotificacion(notificacion));
            }
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
            Pago pago = pagoServicio.simularSegundaCuota(principal.getName(), pagoId);
            Notificacion notificacion = notificarConfirmacionSiCorresponde(principal, pago);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Segunda cuota simulada registrada. Inscripción confirmada automáticamente (sin cobro real).");
            if (notificacion != null) {
                redirectAttributes.addFlashAttribute("estadoNotificacion", textoEstadoNotificacion(notificacion));
            }
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
                              @RequestParam(required = false) String busqueda,
                              Model model) {
        String estadoFiltro = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        model.addAttribute("estadosPago", Arrays.stream(EstadoPago.values()).map(Enum::name).toList());
        model.addAttribute("estadoFiltro", estadoFiltro);
        model.addAttribute("busquedaFiltro", busqueda == null ? "" : busqueda);
        try {
            model.addAttribute("pagos", pagoServicio.filtrarPagosAdmin(estado, busqueda));
        } catch (IllegalArgumentException e) {
            model.addAttribute("pagos", List.of());
            model.addAttribute("error", e.getMessage());
        }
        return "admin/pagos-lista";
    }

    private Notificacion notificarConfirmacionSiCorresponde(Principal principal, Pago pago) {
        if (notificacionServicio == null || asistenteDAO == null || pago.getInscripcion() == null) {
            return null;
        }
        ResumenInscripcion resumen = pagoServicio.calcularResumenPago(pago.getInscripcion());
        if (resumen.estado() != EstadoInscripcion.CONFIRMADA
                || resumen.estadoPago() != EstadoPago.PAGADO || resumen.saldo().signum() != 0) {
            return null;
        }
        return asistenteDAO.findByCorreo(principal.getName())
                .map(asistente -> notificacionServicio.enviarConfirmacionInscripcion(
                        asistente,
                        "Inscripción confirmada",
                        "El pago simulado quedó completo y tu inscripción fue confirmada."))
                .orElse(null);
    }

    private String textoEstadoNotificacion(Notificacion notificacion) {
        return notificacion.getEstadoEnvio() == com.congreso.modelo.enums.EstadoNotificacion.SIMULADO
                ? "Correo simulado; no se realizó una entrega real."
                : notificacion.getEstadoEnvio() == com.congreso.modelo.enums.EstadoNotificacion.ENVIADO
                ? "Correo enviado."
                : "El correo no pudo enviarse.";
    }

}
