package com.congreso.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "administradores")
@PrimaryKeyJoinColumn(name = "codigo_usuario")
public class Administrador extends Usuario {

    @Min(value = 1, message = "El nivel de acceso debe ser al menos 1")
    @Column(name = "nivel_acceso", nullable = false)
    private int nivelAcceso;

    public Administrador() {
        super();
    }

    public Administrador(String nombres, String apellidos, String identificacion,
                         String correo, String telefono, String contrasena, int nivelAcceso) {
        super(nombres, apellidos, identificacion, correo, telefono, contrasena,
                com.congreso.modelo.enums.RolUsuario.ADMINISTRADOR);
        this.nivelAcceso = nivelAcceso;
    }

    public int getNivelAcceso() { return nivelAcceso; }
    public void setNivelAcceso(int nivelAcceso) { this.nivelAcceso = nivelAcceso; }
}
