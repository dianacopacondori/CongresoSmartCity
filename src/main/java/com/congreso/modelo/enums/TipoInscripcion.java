package com.congreso.modelo.enums;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

public enum TipoInscripcion {
    ESTUDIANTE("Estudiante", new BigDecimal("50.00")),
    EXTERNO("Externo", new BigDecimal("100.00"));

    private final String etiqueta;
    private final BigDecimal tarifa;

    TipoInscripcion(String etiqueta, BigDecimal tarifa) {
        this.etiqueta = etiqueta;
        this.tarifa = tarifa;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public BigDecimal tarifa() {
        return tarifa;
    }

    public static TipoInscripcion parsear(String valor) {
        return buscar(valor).orElseThrow(() ->
                new IllegalArgumentException("Tipo de inscripción no válido. Elija Estudiante o Externo."));
    }

    public static Optional<TipoInscripcion> buscar(String valor) {
        if (valor == null || valor.isBlank()) return Optional.empty();
        try {
            return Optional.of(valueOf(valor.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
