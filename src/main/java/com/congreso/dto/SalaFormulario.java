package com.congreso.dto;

import com.congreso.modelo.Sala;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class SalaFormulario {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotNull
    @Positive
    private Integer capacidad;

    @Size(max = 200)
    private String ubicacion;

    public static SalaFormulario desde(Sala sala) {
        SalaFormulario form = new SalaFormulario();
        form.nombre = sala.getNombre();
        form.capacidad = sala.getCapacidad();
        form.ubicacion = sala.getUbicacion();
        return form;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
}
