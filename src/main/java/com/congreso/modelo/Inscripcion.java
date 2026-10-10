package com.congreso.modelo;

import com.congreso.modelo.enums.EstadoInscripcion;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inscripciones")
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_asistente", nullable = false)
    private Asistente asistente;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    @Size(max = 50, message = "El tipo no puede exceder 50 caracteres")
    private String tipo; // Ej: "Estudiante", "Externo"

    @Column(nullable = false)
    private boolean confirmacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInscripcion estado;

    // Relación N a N con Actividad (Genera la tabla intermedia "detalle_inscripcion")
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "detalle_inscripcion",
            joinColumns = @JoinColumn(name = "codigo_inscripcion"),
            inverseJoinColumns = @JoinColumn(name = "codigo_actividad")
    )
    private List<Actividad> actividades = new ArrayList<>();

    public Inscripcion() {
        this.fechaInscripcion = LocalDate.now();
        this.confirmacion = false;
        this.estado = EstadoInscripcion.PENDIENTE;
    }

    public Inscripcion(Asistente asistente, String tipo) {
        this();
        this.asistente = asistente;
        this.tipo = tipo;
    }

    // Getters y Setters
    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Asistente getAsistente() { return asistente; }
    public void setAsistente(Asistente asistente) { this.asistente = asistente; }

    public LocalDate getFechaInscripcion() { return fechaInscripcion; }
    public void setFechaInscripcion(LocalDate fechaInscripcion) { this.fechaInscripcion = fechaInscripcion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public boolean isConfirmacion() { return confirmacion; }
    public void setConfirmacion(boolean confirmacion) { this.confirmacion = confirmacion; }

    public EstadoInscripcion getEstado() { return estado; }
    public void setEstado(EstadoInscripcion estado) { this.estado = estado; }

    public List<Actividad> getActividades() { return actividades; }
    public void setActividades(List<Actividad> actividades) { this.actividades = actividades; }

    /**
     * Agrega una actividad a la inscripción validando cupos y cruces.
     */
    public boolean agregarActividad(Actividad actividad) {
        if (!actividad.hayCupoDisponible()) return false;
        if (validarCrucesHorario(actividad)) return false;

        actividades.add(actividad);
        actividad.incrementarInscritos();
        return true;
    }

    public boolean validarCrucesHorario(Actividad nueva) {
        for (Actividad a : actividades) {
            if (a.cruzaHorarioCon(nueva)) return true;
        }
        return false;
    }

    public void confirmar() {
        this.confirmacion = true;
        this.estado = EstadoInscripcion.CONFIRMADA;
    }
}
