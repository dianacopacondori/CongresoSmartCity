package com.congreso.modelo.enums;

/**
 * Enumeración que representa los diferentes tipos de notificaciones
 * que el sistema puede enviar a los usuarios.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
public enum TipoNotificacion {

    /** Confirmación de que la inscripción al congreso fue registrada. */
    CONFIRMACION_INSCRIPCION,

    /** Envío de la agenda personalizada con las actividades seleccionadas. */
    AGENDA_PERSONALIZADA,

    /** Recordatorio de una actividad próxima a realizarse. */
    RECORDATORIO,

    /** Notificación sobre una corrección de asistencia autorizada por el administrador. */
    CORRECCION_ASISTENCIA,

    /** Confirmación de que un pago o cuota fue registrado exitosamente. */
    CONFIRMACION_PAGO,

    /** Información sobre cambios en el programa del congreso. */
    ACTUALIZACION_PROGRAMA,

    /** Notificación genérica o informativa. */
    INFORMATIVA
}
