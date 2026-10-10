package com.congreso.interfaces;

import com.congreso.modelo.Notificacion;

/**
 * Interface para el envío de notificaciones.
 * Aplica el principio de inversión de dependencias (DIP) de SOLID.
 */
public interface Notificador {

    /**
     * Envía una notificación al destinatario.
     *
     * @param notificacion la notificación a enviar
     * @return true si la notificación se envió correctamente, false en caso contrario
     */
    boolean enviar(Notificacion notificacion);

    /**
     * Verifica si el servicio de notificación está disponible.
     *
     * @return true si el servicio está disponible, false en caso contrario
     */
    boolean estaDisponible();
}
