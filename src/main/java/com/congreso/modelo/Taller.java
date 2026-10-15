package com.congreso.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "talleres")
@PrimaryKeyJoinColumn(name = "codigo_actividad")
public class Taller extends Actividad {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_instructor", nullable = false)
    private Colaborador instructor;

    @Size(max = 500, message = "Los materiales no pueden exceder 500 caracteres")
    @Column(name = "materiales_requeridos", length = 500)
    private String materialesRequeridos;

    @Column(name = "nivel_dificultad", length = 50)
    private String nivelDificultad;

    @Size(max = 500, message = "Los objetivos no pueden exceder 500 caracteres")
    @Column(length = 500)
    private String objetivos;

    public Taller() {
        super();
    }

    public Colaborador getInstructor() { return instructor; }
    public void setInstructor(Colaborador instructor) { this.instructor = instructor; }

    public String getMaterialesRequeridos() { return materialesRequeridos; }
    public void setMaterialesRequeridos(String materialesRequeridos) { this.materialesRequeridos = materialesRequeridos; }

    public String getNivelDificultad() { return nivelDificultad; }
    public void setNivelDificultad(String nivelDificultad) { this.nivelDificultad = nivelDificultad; }

    public String getObjetivos() { return objetivos; }
    public void setObjetivos(String objetivos) { this.objetivos = objetivos; }
}
