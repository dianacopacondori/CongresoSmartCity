package com.congreso.servicio;

import com.congreso.dao.TokenRecuperacionDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.interfaces.Notificador;
import com.congreso.modelo.TokenRecuperacion;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.ResultadoNotificacion;
import com.congreso.modelo.enums.EstadoNotificacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RecuperacionServicio {
    private static final Logger log = LoggerFactory.getLogger(RecuperacionServicio.class);
    private static final String ASUNTO = "Restablecimiento de contraseña";

    private final UsuarioDAO usuarioDAO;
    private final TokenRecuperacionDAO tokenDAO;
    private final PasswordEncoder passwordEncoder;
    private final Notificador notificador;
    private final String baseUrl;

    public RecuperacionServicio(UsuarioDAO usuarioDAO, TokenRecuperacionDAO tokenDAO,
                                PasswordEncoder passwordEncoder, Notificador notificador,
                                @Value("${APP_PUBLIC_URL:http://localhost:8080}") String baseUrl) {
        this.usuarioDAO = usuarioDAO;
        this.tokenDAO = tokenDAO;
        this.passwordEncoder = passwordEncoder;
        this.notificador = notificador;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public void solicitar(String correo) {
        if (correo == null || correo.isBlank()) {
            return;
        }
        usuarioDAO.findByCorreoIgnoreCase(correo.trim()).ifPresent(usuario -> {
            UUID valorToken = UUID.randomUUID();
            TokenRecuperacion token = tokenDAO.saveAndFlush(
                    new TokenRecuperacion(valorToken, usuario.getCodigo(), LocalDateTime.now()));
            String enlace = baseUrl.replaceAll("/+$", "") + "/recuperar/" + token.getToken();
            String cuerpo = "Para restablecer tu contraseña, abre este enlace (válido por 30 minutos):\n" + enlace;
            Runnable enviar = () -> enviarNotificacion(usuario.getCodigo(), usuario.getCorreo(), cuerpo);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        enviar.run();
                    }
                });
            } else {
                enviar.run();
            }
        });
        // TODO: aplicar rate-limit por correo/IP cuando se apruebe la política operativa.
    }

    @Transactional
    public boolean restablecer(UUID valorToken, String nuevaContrasena) {
        if (valorToken == null || nuevaContrasena == null || nuevaContrasena.length() < 8
                || nuevaContrasena.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        TokenRecuperacion token = tokenDAO.bloquearPorToken(valorToken).orElse(null);
        if (token == null || token.isUsado()) {
            return false;
        }
        if (!LocalDateTime.now().isBefore(token.getFechaExpiracion())) {
            token.marcarUsado();
            return false;
        }

        Usuario usuario = usuarioDAO.findById(token.getUsuarioId()).orElse(null);
        token.marcarUsado();
        if (usuario == null) {
            return false;
        }
        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioDAO.save(usuario);
        return true;
    }

    private void enviarNotificacion(Long usuarioId, String correo, String cuerpo) {
        try {
            ResultadoNotificacion resultado = notificador.enviar(correo, ASUNTO, cuerpo);
            if (resultado != null && resultado.estado() == EstadoNotificacion.FALLIDO) {
                log.error("Falló el envío de recuperación para usuario id {}", usuarioId);
            }
        } catch (RuntimeException e) {
            log.error("Falló el envío de recuperación para usuario id {}", usuarioId, e);
        }
    }
}
