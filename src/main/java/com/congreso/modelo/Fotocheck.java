package com.congreso.modelo;

import com.congreso.modelo.enums.EstadoFotocheck;
import com.congreso.modelo.enums.RolUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Entidad que representa el fotocheck digital del participante.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Entity
@Table(name = "fotochecks")
public class Fotocheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100)
    @Column(nullable = false)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100)
    @Column(nullable = false)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false)
    private RolUsuario tipoUsuario;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoFotocheck estado;

    public Fotocheck() {
        this.fechaEmision = LocalDate.now();
        this.estado = EstadoFotocheck.EMITIDO;
    }

    public Fotocheck(Usuario usuario) {
        this();
        this.usuario = usuario;
        this.nombres = usuario.getNombres();
        this.apellidos = usuario.getApellidos();
        this.tipoUsuario = usuario.getRol();
    }

    // Getters y Setters
    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public RolUsuario getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(RolUsuario tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public EstadoFotocheck getEstado() { return estado; }
    public void setEstado(EstadoFotocheck estado) { this.estado = estado; }
}