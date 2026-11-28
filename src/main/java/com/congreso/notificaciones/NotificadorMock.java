package com.congreso.notificaciones;

import com.congreso.interfaces.Notificador;
import com.congreso.modelo.ResultadoNotificacion;
import com.congreso.modelo.enums.EstadoNotificacion;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementación mock del notificador para entorno de desarrollo/pruebas.
 * Simula el envío de correos sin conectarse a un servidor SMTP real.
 * Se activa automáticamente cuando el perfil de Spring es "dev" o "test".
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Component
@Profile({"dev", "test", "default"})
public class NotificadorMock implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(NotificadorMock.class);

    @Override
    public ResultadoNotificacion enviar(String destinatario, String asunto, String cuerpo) {
        log.info("=== [MOCK EMAIL] ===");
        log.info("Para: {}", destinatario);
        log.info("Asunto: {}", asunto);
        log.info("Contenido: {}", cuerpo == null ? null
                : cuerpo.replaceAll("(/recuperar/)[0-9a-fA-F-]{36}", "$1[REDACTED]"));
        log.info("====================");
        return new ResultadoNotificacion(EstadoNotificacion.SIMULADO, LocalDateTime.now());
    }

    @Override
    public boolean estaDisponible() {
        return true;
    }
}
