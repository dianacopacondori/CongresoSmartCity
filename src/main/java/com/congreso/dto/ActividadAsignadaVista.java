package com.congreso.dto;

import com.congreso.modelo.Actividad;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.enums.EstadoActividad;

import java.time.LocalDate;
import java.time.LocalTime;

public class ActividadAsignadaVista {

    private final Long codigo;
    private final String tipo;
    private final String titulo;
    private final LocalDate fecha;
    private final LocalTime horarioInicio;
    private final LocalTime horarioFin;
    private final String sala;
    private final EstadoActividad estado;

    private ActividadAsignadaVista(Long codigo, String tipo, String titulo, LocalDate fecha,
                                   LocalTime horarioInicio, LocalTime horarioFin, String sala,
                                   EstadoActividad estado) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.titulo = titulo;
        this.fecha = fecha;
        this.horarioInicio = horarioInicio;
        this.horarioFin = horarioFin;
        this.sala = sala;
        this.estado = estado;
    }

    public static ActividadAsignadaVista desde(Actividad actividad) {
        return new ActividadAsignadaVista(
                actividad.getCodigo(),
                actividad instanceof Ponencia ? "Ponencia" : "Taller",
                actividad.getTitulo(),
                actividad.getFecha(),
                actividad.getHorarioInicio(),
                actividad.getHorarioFin(),
                actividad.getSala().getNombre(),
                actividad.getEstado());
    }

    public Long getCodigo() { return codigo; }
    public String getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHorarioInicio() { return horarioInicio; }
    public LocalTime getHorarioFin() { return horarioFin; }
    public String getSala() { return sala; }
    public EstadoActividad getEstado() { return estado; }
}
