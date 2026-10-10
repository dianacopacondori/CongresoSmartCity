package com.congreso.modelo;

import com.congreso.modelo.enums.EstadoNotificacion;
import com.congreso.modelo.enums.TipoNotificacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entidad que representa las notificaciones enviadas a los usuarios.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_usuario", nullable = false)
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 200)
    @Column(nullable = false)
    private String asunto;

    @NotBlank(message = "El contenido es obligatorio")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "horario_personalizado", columnDefinition = "TEXT")
    private String horarioPersonalizado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false)
    private EstadoNotificacion estadoEnvio;

    public Notificacion() {
        this.fecha = LocalDateTime.now();
        this.estadoEnvio = EstadoNotificacion.PENDIENTE;
    }

    public Notificacion(Usuario destinatario, TipoNotificacion tipo, String asunto, String contenido) {
        this();
        this.destinatario = destinatario;
        this.tipo = tipo;
        this.asunto = asunto;
        this.contenido = contenido;
    }

    // Getters y Setters
    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Usuario getDestinatario() { return destinatario; }
    public void setDestinatario(Usuario destinatario) { this.destinatario = destinatario; }

    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getHorarioPersonalizado() { return horarioPersonalizado; }
    public void setHorarioPersonalizado(String horarioPersonalizado) { this.horarioPersonalizado = horarioPersonalizado; }

    public EstadoNotificacion getEstadoEnvio() { return estadoEnvio; }
    public void setEstadoEnvio(EstadoNotificacion estadoEnvio) { this.estadoEnvio = estadoEnvio; }

    public void registrarEstado(EstadoNotificacion estado) {
        this.estadoEnvio = estado;
    }
}
