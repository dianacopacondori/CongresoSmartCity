package com.congreso.controlador;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Fotocheck;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Usuario;
import com.congreso.servicio.FotocheckServicio;
import com.congreso.servicio.InscripcionServicio;
import com.congreso.util.GeneradorPdfSimple;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Recorrido usable del fotocheck: emisión para asistentes elegibles, descarga digital
 * y verificación por parte del personal de control de asistencia.
 */
@Controller
@RequestMapping("/fotocheck")
public class FotocheckController {

    private final FotocheckServicio fotocheckServicio;
    private final InscripcionServicio inscripcionServicio;
    private final UsuarioDAO usuarioDAO;

    public FotocheckController(FotocheckServicio fotocheckServicio,
                               InscripcionServicio inscripcionServicio,
                               UsuarioDAO usuarioDAO) {
        this.fotocheckServicio = fotocheckServicio;
        this.inscripcionServicio = inscripcionServicio;
        this.usuarioDAO = usuarioDAO;
    }

    @GetMapping("/mi-fotocheck")
    public String miFotocheck(Principal principal, Model model) {
        try {
            model.addAttribute("fotocheck", emitirPara(principal));
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "fotocheck/mi-fotocheck";
    }

    @GetMapping(value = "/descargar", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargar(Principal principal) {
        Fotocheck fotocheck = emitirPara(principal);
        List<String> lineas = List.of(
                "CONGRESO INTERNACIONAL SMART CITY",
                "FOTOCHECK DIGITAL",
                "Codigo: " + fotocheck.getCodigo(),
                "Asistente: " + fotocheck.getNombres() + " " + fotocheck.getApellidos(),
                "Rol: " + fotocheck.getTipoUsuario(),
                "Emitido: " + fotocheck.getFechaEmision(),
                "Estado: " + fotocheck.getEstado(),
                "Presente este codigo en el control de asistencia.");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("fotocheck_" + fotocheck.getCodigo() + ".pdf").build());
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        return new ResponseEntity<>(GeneradorPdfSimple.generar(lineas, 52), headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping("/admin/verificar")
    public String formularioVerificar() {
        return "fotocheck/verificar";
    }

    @PostMapping("/admin/verificar")
    public String verificar(@RequestParam Long codigo, Model model) {
        Optional<Fotocheck> fotocheck = fotocheckServicio.buscarPorCodigo(codigo);
        boolean valido = fotocheck.isPresent() && fotocheckServicio.validarFotocheck(codigo);
        model.addAttribute("codigoConsultado", codigo);
        model.addAttribute("valido", valido);
        model.addAttribute("fotocheck", valido ? fotocheck.get() : null);
        if (!valido) {
            model.addAttribute("error", "El codigo no corresponde a un fotocheck emitido y vigente.");
        }
        return "fotocheck/verificar";
    }

    /** Emite (o renueva) solo si hay inscripción confirmada con pago simulado completo. */
    private Fotocheck emitirPara(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Debe iniciar sesión para ver su fotocheck");
        }
        Usuario usuario = usuarioDAO.findByCorreo(principal.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró la cuenta autenticada"));
        if (!usuario.estaActivo()) {
            throw new IllegalStateException("Su cuenta no está activa");
        }
        Optional<Inscripcion> elegible = inscripcionServicio.inscripcionDisponible(usuario.getCorreo());
        if (elegible.isEmpty()) {
            throw new IllegalStateException(
                    "Necesita una inscripción confirmada con el pago simulado completo para obtener el fotocheck");
        }
        return fotocheckServicio.generarFotocheck(usuario);
    }
}
