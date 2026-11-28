package com.congreso.notificaciones;

import com.congreso.interfaces.Notificador;
import com.congreso.modelo.ResultadoNotificacion;
import com.congreso.modelo.enums.EstadoNotificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("prod")
public class NotificadorSmtp implements Notificador {
    private static final Logger log = LoggerFactory.getLogger(NotificadorSmtp.class);

    // TODO D06: confirmar medio SMTP de prueba y criterio definitivo de "entregado".

    private final JavaMailSender mailSender;
    private final String remitente;

    public NotificadorSmtp(JavaMailSender mailSender,
                            @Value("${MAIL_USERNAME}") String usuarioSmtp,
                            @Value("${MAIL_PASSWORD}") String passwordSmtp) {
        if (usuarioSmtp == null || usuarioSmtp.isBlank()) {
            throw new IllegalStateException("MAIL_USERNAME es obligatorio en perfil prod");
        }
        if (passwordSmtp == null || passwordSmtp.isBlank()) {
            throw new IllegalStateException("MAIL_PASSWORD es obligatorio en perfil prod");
        }
        this.mailSender = mailSender;
        this.remitente = usuarioSmtp;
    }

    @Override
    public ResultadoNotificacion enviar(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
            return new ResultadoNotificacion(EstadoNotificacion.ENVIADO, LocalDateTime.now());
        } catch (MailException e) {
            log.error("Fallo SMTP al notificar a {}", destinatario, e);
            return new ResultadoNotificacion(EstadoNotificacion.FALLIDO, LocalDateTime.now());
        }
    }

    @Override
    public boolean estaDisponible() {
        return true;
    }
}
