package com.congreso.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "ponencias")
@PrimaryKeyJoinColumn(name = "codigo_actividad")
public class Ponencia extends Actividad {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_ponente", nullable = false)
    private Colaborador ponente;

    @Size(max = 200, message = "El tema no puede exceder 200 caracteres")
    @Column(length = 200)
    private String tema;

    @Size(max = 500, message = "El resumen no puede exceder 500 caracteres")
    @Column(length = 500)
    private String resumen;

    @Column(name = "nivel_academico", length = 50)
    private String nivelAcademico;

    public Ponencia() {
        super();
    }

    public Colaborador getPonente() { return ponente; }
    public void setPonente(Colaborador ponente) { this.ponente = ponente; }

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }

    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }

    public String getNivelAcademico() { return nivelAcademico; }
    public void setNivelAcademico(String nivelAcademico) { this.nivelAcademico = nivelAcademico; }
}
