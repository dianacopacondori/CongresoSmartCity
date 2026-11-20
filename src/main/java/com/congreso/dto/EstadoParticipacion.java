package com.congreso.dto;

/** Resumen de una sola inscripción y su pago, para guiar al asistente sin mezclar registros. */
public record EstadoParticipacion(String inscripcion, String pago, Paso paso, Long codigoInscripcion) {
    public enum Paso {
        CREAR_INSCRIPCION,
        SIMULAR_PAGO,
        COMPLETAR_CUOTA,
        VER_AGENDA,
        REVISAR_ESTADO
    }
}
