package com.congreso.modelo;

import com.congreso.modelo.enums.EstadoNotificacion;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones_enviadas")
public class NotificacionEnviada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Column(nullable = false, length = 254)
    private String destinatario;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNotificacion estado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "evento_origen", nullable = false, length = 80)
    private String eventoOrigen;

    public NotificacionEnviada() {}

    public NotificacionEnviada(String destinatario, String asunto, EstadoNotificacion estado,
                               LocalDateTime fecha, String eventoOrigen) {
        this.destinatario = destinatario;
        this.asunto = asunto;
        this.estado = estado;
        this.fecha = fecha;
        this.eventoOrigen = eventoOrigen;
    }

    public Long getCodigo() { return codigo; }
    public String getDestinatario() { return destinatario; }
    public String getAsunto() { return asunto; }
    public EstadoNotificacion getEstado() { return estado; }
    public LocalDateTime getFecha() { return fecha; }
    public String getEventoOrigen() { return eventoOrigen; }
}
