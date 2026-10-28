package com.congreso.notificaciones;

import com.congreso.interfaces.Notificador;
import com.congreso.modelo.Notificacion;
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
    public boolean enviar(Notificacion notificacion) {
        log.info("=== [MOCK EMAIL] ===");
        log.info("Para: {}", notificacion.getDestinatario().getCorreo());
        log.info("Asunto: {}", notificacion.getAsunto());
        log.info("Contenido: {}", notificacion.getContenido());
        log.info("====================");
        return true;
    }

    @Override
    public boolean estaDisponible() {
        return true;
    }
}
