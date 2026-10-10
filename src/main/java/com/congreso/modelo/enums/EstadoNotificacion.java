package com.congreso.modelo.enums;

/**
 * Enumeración que representa los estados posibles de una notificación.
 * Permite rastrear si el correo fue enviado correctamente, está pendiente o falló.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
public enum EstadoNotificacion {

    /** La notificación está en cola pero aún no se ha enviado. */
    PENDIENTE,

    /** La notificación fue enviada exitosamente al destinatario. */
    ENVIADO,

    /** El envío de la notificación falló (error de SMTP, correo inválido, etc.). */
    FALLIDO,

    /** La notificación fue cancelada o anulada antes de su envío. */
    CANCELADO
}
