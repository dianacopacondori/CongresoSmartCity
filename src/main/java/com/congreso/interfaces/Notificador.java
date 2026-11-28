package com.congreso.interfaces;

import com.congreso.modelo.Notificacion;
import com.congreso.modelo.ResultadoNotificacion;

/**
 * Interface para el envío de notificaciones.
 * Aplica el principio de inversión de dependencias (DIP) de SOLID.
 */
public interface Notificador {

    /**
     * Envía una notificación al destinatario.
     *
     * @param notificacion la notificación a enviar
     * @return resultado con estado y fecha del intento
     */
    ResultadoNotificacion enviar(String destinatario, String asunto, String cuerpo);

    /**
     * Verifica si el servicio de notificación está disponible.
     *
     * @return true si el servicio está disponible, false en caso contrario
     */
    boolean estaDisponible();
}
