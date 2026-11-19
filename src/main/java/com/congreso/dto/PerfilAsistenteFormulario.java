package com.congreso.dto;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.enums.ModalidadPago;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PerfilAsistenteFormulario {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden exceder 100 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    private String apellidos;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    @Size(max = 100, message = "La procedencia no puede exceder 100 caracteres")
    private String procedencia;

    @Size(max = 100, message = "La ocupación no puede exceder 100 caracteres")
    private String ocupacion;

    @Size(max = 100, message = "La universidad no puede exceder 100 caracteres")
    private String universidad;

    @Size(max = 100, message = "La empresa no puede exceder 100 caracteres")
    private String empresa;

    @Min(value = 16, message = "La edad mínima es 16 años")
    @Max(value = 100, message = "La edad máxima es 100 años")
    private Integer edad;

    private ModalidadPago modalidadPagoPreferida;

    public static PerfilAsistenteFormulario desde(Asistente asistente) {
        PerfilAsistenteFormulario perfil = new PerfilAsistenteFormulario();
        perfil.nombres = asistente.getNombres();
        perfil.apellidos = asistente.getApellidos();
        perfil.telefono = asistente.getTelefono();
        perfil.procedencia = asistente.getProcedencia();
        perfil.ocupacion = asistente.getOcupacion();
        perfil.universidad = asistente.getUniversidad();
        perfil.empresa = asistente.getEmpresa();
        perfil.edad = asistente.getEdad();
        perfil.modalidadPagoPreferida = asistente.getModalidadPagoPreferida();
        return perfil;
    }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getProcedencia() { return procedencia; }
    public void setProcedencia(String procedencia) { this.procedencia = procedencia; }
    public String getOcupacion() { return ocupacion; }
    public void setOcupacion(String ocupacion) { this.ocupacion = ocupacion; }
    public String getUniversidad() { return universidad; }
    public void setUniversidad(String universidad) { this.universidad = universidad; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }
    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }
    public ModalidadPago getModalidadPagoPreferida() { return modalidadPagoPreferida; }
    public void setModalidadPagoPreferida(ModalidadPago modalidadPagoPreferida) { this.modalidadPagoPreferida = modalidadPagoPreferida; }
}
