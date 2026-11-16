package com.congreso.dto;

import com.congreso.modelo.Actividad;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Taller;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class ActividadFormulario {

    private Long codigo;

    @NotBlank
    @Pattern(regexp = "PONENCIA|TALLER")
    private String tipo = "PONENCIA";

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @Size(max = 1000)
    private String descripcion;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioInicio;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horarioFin;

    @NotNull
    @Min(1)
    private Integer cupo;

    @NotNull
    private Long salaId;

    @NotNull
    private Long colaboradorId;

    @Size(max = 200)
    private String tema;

    @Size(max = 50)
    private String nivelDificultad;

    @Size(max = 500)
    private String materialesRequeridos;

    @Size(max = 500)
    private String objetivos;

    public static ActividadFormulario desde(Actividad actividad) {
        ActividadFormulario form = new ActividadFormulario();
        form.codigo = actividad.getCodigo();
        form.titulo = actividad.getTitulo();
        form.descripcion = actividad.getDescripcion();
        form.fecha = actividad.getFecha();
        form.horarioInicio = actividad.getHorarioInicio();
        form.horarioFin = actividad.getHorarioFin();
        form.cupo = actividad.getCupo();
        form.salaId = actividad.getSala().getCodigo();
        if (actividad instanceof Ponencia ponencia) {
            form.tipo = "PONENCIA";
            form.colaboradorId = ponencia.getPonente().getCodigo();
            form.tema = ponencia.getTema();
        } else if (actividad instanceof Taller taller) {
            form.tipo = "TALLER";
            form.colaboradorId = taller.getInstructor().getCodigo();
            form.nivelDificultad = taller.getNivelDificultad();
            form.materialesRequeridos = taller.getMaterialesRequeridos();
            form.objetivos = taller.getObjetivos();
        } else {
            throw new IllegalArgumentException("Tipo de actividad no admitido");
        }
        return form;
    }

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHorarioInicio() { return horarioInicio; }
    public void setHorarioInicio(LocalTime horarioInicio) { this.horarioInicio = horarioInicio; }
    public LocalTime getHorarioFin() { return horarioFin; }
    public void setHorarioFin(LocalTime horarioFin) { this.horarioFin = horarioFin; }
    public Integer getCupo() { return cupo; }
    public void setCupo(Integer cupo) { this.cupo = cupo; }
    public Long getSalaId() { return salaId; }
    public void setSalaId(Long salaId) { this.salaId = salaId; }
    public Long getColaboradorId() { return colaboradorId; }
    public void setColaboradorId(Long colaboradorId) { this.colaboradorId = colaboradorId; }
    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
    public String getNivelDificultad() { return nivelDificultad; }
    public void setNivelDificultad(String nivelDificultad) { this.nivelDificultad = nivelDificultad; }
    public String getMaterialesRequeridos() { return materialesRequeridos; }
    public void setMaterialesRequeridos(String materialesRequeridos) { this.materialesRequeridos = materialesRequeridos; }
    public String getObjetivos() { return objetivos; }
    public void setObjetivos(String objetivos) { this.objetivos = objetivos; }
}
