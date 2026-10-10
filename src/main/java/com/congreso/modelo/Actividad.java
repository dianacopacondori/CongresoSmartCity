package com.congreso.modelo;

import com.congreso.modelo.enums.EstadoActividad;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad abstracta que representa una actividad del congreso.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Entity
@Table(name = "actividades")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede exceder 200 caracteres")
    @Column(nullable = false)
    private String titulo;

    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoActividad estado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "horario_inicio", nullable = false)
    private LocalTime horarioInicio;

    @Column(name = "horario_fin", nullable = false)
    private LocalTime horarioFin;

    @Min(value = 1, message = "El cupo mínimo es 1")
    @Column(nullable = false)
    private Integer cupo;

    @Column(nullable = false)
    private Integer inscritos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_sala", nullable = false)
    private Sala sala;

    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Asistencia> asistencias = new ArrayList<>();

    // Constructor por defecto
    public Actividad() {
        this.estado = EstadoActividad.ACTIVA;
        this.inscritos = 0;
    }

    // Constructor con parámetros
    public Actividad(String titulo, String descripcion, LocalDate fecha,
                     LocalTime horarioInicio, LocalTime horarioFin,
                     Integer cupo, Sala sala) {
        this();
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.horarioInicio = horarioInicio;
        this.horarioFin = horarioFin;
        this.cupo = cupo;
        this.sala = sala;
    }

    // Getters y Setters
    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoActividad getEstado() {
        return estado;
    }

    public void setEstado(EstadoActividad estado) {
        this.estado = estado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(LocalTime horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public LocalTime getHorarioFin() {
        return horarioFin;
    }

    public void setHorarioFin(LocalTime horarioFin) {
        this.horarioFin = horarioFin;
    }

    public Integer getCupo() {
        return cupo;
    }

    public void setCupo(Integer cupo) {
        this.cupo = cupo;
    }

    public Integer getInscritos() {
        return inscritos;
    }

    public void setInscritos(Integer inscritos) {
        this.inscritos = inscritos;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public List<Asistencia> getAsistencias() {
        return asistencias;
    }

    public void setAsistencias(List<Asistencia> asistencias) {
        this.asistencias = asistencias;
    }

    /**
     * Verifica si hay cupo disponible para la actividad.
     *
     * @return true si hay cupo disponible, false en caso contrario
     */
    public boolean hayCupoDisponible() {
        return this.inscritos < this.cupo && this.estado == EstadoActividad.ACTIVA;
    }

    /**
     * Verifica si la actividad cruza horario con otra actividad.
     *
     * @param otra la otra actividad a comparar
     * @return true si hay cruce de horario, false en caso contrario
     */
    public boolean cruzaHorarioCon(Actividad otra) {
        if (!this.fecha.equals(otra.fecha)) {
            return false;
        }
        return this.horarioInicio.isBefore(otra.horarioFin) &&
                this.horarioFin.isAfter(otra.horarioInicio);
    }

    /**
     * Incrementa el número de inscritos en 1.
     */
    public void incrementarInscritos() {
        this.inscritos++;
    }

    /**
     * Decrementa el número de inscritos en 1.
     */
    public void decrementarInscritos() {
        if (this.inscritos > 0) {
            this.inscritos--;
        }
    }

    @Override
    public String toString() {
        return "Actividad{" +
                "codigo=" + codigo +
                ", titulo='" + titulo + '\'' +
                ", fecha=" + fecha +
                ", horarioInicio=" + horarioInicio +
                ", horarioFin=" + horarioFin +
                ", cupo=" + cupo +
                ", inscritos=" + inscritos +
                ", estado=" + estado +
                '}';
    }
}
