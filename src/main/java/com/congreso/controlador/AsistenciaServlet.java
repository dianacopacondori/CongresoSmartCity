package com.congreso.controlador;

import com.congreso.modelo.Administrador;
import com.congreso.modelo.enums.EstadoAsistencia;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.AsistenciaServicio;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/asistencias")
public class AsistenciaServlet {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDITORIA");

    private final AsistenciaServicio asistenciaServicio;

    public AsistenciaServlet(AsistenciaServicio asistenciaServicio) {
        this.asistenciaServicio = asistenciaServicio;
    }

    @PostMapping("/{codigo}/correccion")
    public ResponseEntity<Void> corregirAsistencia(
            @PathVariable Long codigo,
            @RequestParam EstadoAsistencia estado,
            @RequestParam String motivo,
            HttpSession session) {
        Object usuarioSesion = session.getAttribute("usuario");
        if (!(usuarioSesion instanceof Administrador administrador)
                || administrador.getRol() != RolUsuario.ADMINISTRADOR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String motivoAuditoria = sanitizarCampoAuditoria(motivo);
        if (motivoAuditoria.isBlank() || motivoAuditoria.length() > 500
                || (estado != EstadoAsistencia.PRESENTE && estado != EstadoAsistencia.AUSENTE)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            asistenciaServicio.corregirAsistencia(codigo, estado);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }

        String nombreAdministrador = sanitizarCampoAuditoria(
                administrador.getNombres() + " " + administrador.getApellidos());
        String usuarioAnterior = MDC.get("usuario");
        try {
            MDC.put("usuario", nombreAdministrador);
            auditLog.info("Corrección de asistencia asistenciaId={} administradorId={} estado={} motivo={}",
                    codigo, administrador.getCodigo(), estado, motivoAuditoria);
        } finally {
            if (usuarioAnterior == null) {
                MDC.remove("usuario");
            } else {
                MDC.put("usuario", usuarioAnterior);
            }
        }

        return ResponseEntity.noContent().build();
    }

    private String sanitizarCampoAuditoria(String valor) {
        return valor == null ? "" : valor.replaceAll("[\\r\\n\\p{Cntrl}]+", " ").trim();
    }
}
