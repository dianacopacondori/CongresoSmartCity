package com.congreso.dto;

import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;

import java.math.BigDecimal;

public record ResumenInscripcion(
        EstadoInscripcion estado,
        EstadoPago estadoPago,
        ModalidadPago modalidadPago,
        BigDecimal tarifa,
        BigDecimal saldo) {
}
