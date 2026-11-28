package com.congreso.modelo;

import com.congreso.modelo.enums.EstadoNotificacion;
import java.time.LocalDateTime;

public record ResultadoNotificacion(EstadoNotificacion estado, LocalDateTime fecha) {
}
