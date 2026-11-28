package com.congreso.modelo;

import com.congreso.modelo.enums.EstadoAsistencia;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad que representa el registro de asistencia de un participante a una actividad.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Entity
@Table(name = "asistencias", uniqueConstraints = @UniqueConstraint(
        name = "uk_asistencia_asistente_actividad",
        columnNames = {"codigo_asistente", "codigo_actividad"}))
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_asistente", nullable = false)
    private Asistente asistente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_actividad", nullable = false)
    private Actividad actividad;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_registro", nullable = false)
    private LocalTime hora;

    @Column(name = "mecanismo_validacion", length = 50)
    private String mecanismoValidacion; // Ej: "QR", "CODIGO", "MANUAL"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAsistencia estado;

    /** Personal que registró la asistencia; permite trazabilidad de la corrección. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;

    @Column(name = "justificacion_correccion", length = 500)
    private String justificacionCorreccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corregido_por")
    private Usuario corregidoPor;

    @Column(name = "fecha_correccion")
    private LocalDateTime fechaCorreccion;

    // Constructor por defecto (OBLIGATORIO para JPA)
    public Asistencia() {
    }

    // Constructor con parámetros
    public Asistencia(Asistente asistente, Actividad actividad, LocalDate fecha, LocalTime hora, String mecanismoValidacion, EstadoAsistencia estado) {
        this.asistente = asistente;
        this.actividad = actividad;
        this.fecha = fecha;
        this.hora = hora;
        this.mecanismoValidacion = mecanismoValidacion;
        this.estado = estado;
    }

    // Getters y Setters
    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Asistente getAsistente() { return asistente; }
    public void setAsistente(Asistente asistente) { this.asistente = asistente; }

    public Actividad getActividad() { return actividad; }
    public void setActividad(Actividad actividad) { this.actividad = actividad; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }

    public String getMecanismoValidacion() { return mecanismoValidacion; }
    public void setMecanismoValidacion(String mecanismoValidacion) { this.mecanismoValidacion = mecanismoValidacion; }

    public EstadoAsistencia getEstado() { return estado; }
    public void setEstado(EstadoAsistencia estado) { this.estado = estado; }

    public Usuario getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Usuario registradoPor) { this.registradoPor = registradoPor; }

    public String getJustificacionCorreccion() { return justificacionCorreccion; }
    public void setJustificacionCorreccion(String justificacionCorreccion) {
        this.justificacionCorreccion = justificacionCorreccion;
    }

    public Usuario getCorregidoPor() { return corregidoPor; }
    public void setCorregidoPor(Usuario corregidoPor) { this.corregidoPor = corregidoPor; }

    public LocalDateTime getFechaCorreccion() { return fechaCorreccion; }
    public void setFechaCorreccion(LocalDateTime fechaCorreccion) { this.fechaCorreccion = fechaCorreccion; }
}
