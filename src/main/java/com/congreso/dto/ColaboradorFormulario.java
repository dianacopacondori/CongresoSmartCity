package com.congreso.dto;

import com.congreso.modelo.Colaborador;
import com.congreso.modelo.enums.EstadoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ColaboradorFormulario {

    @NotBlank
    @Size(max = 100)
    private String nombres;

    @NotBlank
    @Size(max = 100)
    private String apellidos;

    @NotBlank
    @Size(max = 20)
    private String identificacion;

    @NotBlank
    @Email
    @Size(max = 254)
    private String correo;

    @Size(max = 20)
    private String telefono;

    @Size(max = 150)
    private String institucion;

    @Size(max = 150)
    private String empresa;

    @NotBlank
    @Pattern(regexp = "COLABORADOR|INSTRUCTOR|PONENTE")
    private String tipoColaborador = "COLABORADOR";

    @NotNull
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Size(max = 72)
    private String contrasena;

    public static ColaboradorFormulario desde(Colaborador colaborador) {
        ColaboradorFormulario form = new ColaboradorFormulario();
        form.nombres = colaborador.getNombres();
        form.apellidos = colaborador.getApellidos();
        form.identificacion = colaborador.getIdentificacion();
        form.correo = colaborador.getCorreo();
        form.telefono = colaborador.getTelefono();
        form.institucion = colaborador.getInstitucion();
        form.empresa = colaborador.getEmpresa();
        form.tipoColaborador = colaborador.getTipoColaborador().name();
        form.estado = colaborador.getEstado();
        return form;
    }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getInstitucion() { return institucion; }
    public void setInstitucion(String institucion) { this.institucion = institucion; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }
    public String getTipoColaborador() { return tipoColaborador; }
    public void setTipoColaborador(String tipoColaborador) { this.tipoColaborador = tipoColaborador; }
    public EstadoUsuario getEstado() { return estado; }
    public void setEstado(EstadoUsuario estado) { this.estado = estado; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}
