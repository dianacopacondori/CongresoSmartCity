package com.congreso.modelo;

import com.congreso.modelo.enums.ModalidadPago;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asistentes")
@PrimaryKeyJoinColumn(name = "codigo_usuario")
public class Asistente extends Usuario {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_pago_preferida")
    private ModalidadPago modalidadPagoPreferida;

    @OneToMany(mappedBy = "asistente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones = new ArrayList<>();

    @OneToMany(mappedBy = "asistente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Asistencia> asistencias = new ArrayList<>();

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Fotocheck fotocheck;

    // Constructor por defecto
    public Asistente() {
        super();
    }

    // Constructor con parámetros
    public Asistente(String nombres, String apellidos, String identificacion,
                     String correo, String telefono, String contrasena,
                     String procedencia, String ocupacion, String universidad,
                     String empresa, Integer edad, ModalidadPago modalidadPagoPreferida) {
        super(nombres, apellidos, identificacion, correo, telefono, contrasena,
                com.congreso.modelo.enums.RolUsuario.ASISTENTE);
        this.procedencia = procedencia;
        this.ocupacion = ocupacion;
        this.universidad = universidad;
        this.empresa = empresa;
        this.edad = edad;
        this.modalidadPagoPreferida = modalidadPagoPreferida;
    }

    // Getters y Setters
    public String getProcedencia() {
        return procedencia;
    }

    public void setProcedencia(String procedencia) {
        this.procedencia = procedencia;
    }

    public String getOcupacion() {
        return ocupacion;
    }

    public void setOcupacion(String ocupacion) {
        this.ocupacion = ocupacion;
    }

    public String getUniversidad() {
        return universidad;
    }

    public void setUniversidad(String universidad) {
        this.universidad = universidad;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public ModalidadPago getModalidadPagoPreferida() {
        return modalidadPagoPreferida;
    }

    public void setModalidadPagoPreferida(ModalidadPago modalidadPagoPreferida) {
        this.modalidadPagoPreferida = modalidadPagoPreferida;
    }

    public List<Inscripcion> getInscripciones() {
        return inscripciones;
    }

    public void setInscripciones(List<Inscripcion> inscripciones) {
        this.inscripciones = inscripciones;
    }

    public List<Asistencia> getAsistencias() {
        return asistencias;
    }

    public void setAsistencias(List<Asistencia> asistencias) {
        this.asistencias = asistencias;
    }

    public Fotocheck getFotocheck() {
        return fotocheck;
    }

    public void setFotocheck(Fotocheck fotocheck) {
        this.fotocheck = fotocheck;
    }

    @Override
    public String toString() {
        return "Asistente{" +
                "procedencia='" + procedencia + '\'' +
                ", ocupacion='" + ocupacion + '\'' +
                ", universidad='" + universidad + '\'' +
                ", empresa='" + empresa + '\'' +
                ", edad=" + edad +
                "} " + super.toString();
    }
}