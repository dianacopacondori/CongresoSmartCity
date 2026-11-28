package com.congreso.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tokens_recuperacion")
public class TokenRecuperacion {
    @Id
    @Column(name = "token", nullable = false, updatable = false)
    private UUID token;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean usado;

    protected TokenRecuperacion() {
    }

    public TokenRecuperacion(UUID token, Long usuarioId, LocalDateTime fechaCreacion) {
        this.token = token;
        this.usuarioId = usuarioId;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracion = fechaCreacion.plusMinutes(30);
        this.usado = false;
    }

    public UUID getToken() {
        return token;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public boolean isUsado() {
        return usado;
    }

    public void marcarUsado() {
        this.usado = true;
    }
}
