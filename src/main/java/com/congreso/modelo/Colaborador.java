package com.congreso.modelo;

import com.congreso.modelo.enums.RolUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

/**
 * Entidad que representa a un colaborador, instructor o ponente.
 *
 * @author cvdiana
 * @version 1.0.0
 */
@Entity
@Table(name = "colaboradores")
@PrimaryKeyJoinColumn(name = "codigo_usuario")
public class Colaborador extends Usuario {

    @Size(max = 150, message = "La institución no puede exceder 150 caracteres")
    private String institucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_colaborador", nullable = false)
    private RolUsuario tipoColaborador; // PONENTE, INSTRUCTOR, COLABORADOR

    // NOTA: Si ya creaste las clases Ponencia y Taller, puedes descomentar estas relaciones:
    /*
    @OneToMany(mappedBy = "ponente", cascade = CascadeType.ALL)
    private List<Ponencia> ponencias;

    @OneToMany(mappedBy = "responsable", cascade = CascadeType.ALL)
    private List<Taller> talleres;
    */

    public Colaborador() {
        super();
    }

    public Colaborador(String nombres, String apellidos, String identificacion,
                       String correo, String telefono, String contrasena,
                       String institucion, RolUsuario tipoColaborador) {
        super(nombres, apellidos, identificacion, correo, telefono, contrasena, tipoColaborador);
        this.institucion = institucion;
        this.tipoColaborador = tipoColaborador;
    }

    public String getInstitucion() { return institucion; }
    public void setInstitucion(String institucion) { this.institucion = institucion; }

    public RolUsuario getTipoColaborador() { return tipoColaborador; }
    public void setTipoColaborador(RolUsuario tipoColaborador) { this.tipoColaborador = tipoColaborador; }
}