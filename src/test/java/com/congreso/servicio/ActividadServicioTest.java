package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.ColaboradorDAO;
import com.congreso.dao.SalaDAO;
import com.congreso.dto.ActividadFormulario;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Sala;
import com.congreso.modelo.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActividadServicioTest {

    private ActividadDAO actividadDAO;
    private SalaDAO salaDAO;
    private ColaboradorDAO colaboradorDAO;
    private ActividadServicio servicio;

    @BeforeEach
    void setUp() {
        actividadDAO = mock(ActividadDAO.class);
        salaDAO = mock(SalaDAO.class);
        colaboradorDAO = mock(ColaboradorDAO.class);
        servicio = new ActividadServicio(actividadDAO, salaDAO, colaboradorDAO);
    }

    @Test
    void creaPonenciaConSalaYResponsableValidos() {
        ActividadFormulario form = formulario();
        Sala sala = new Sala("Sala A", 30, "Piso 1");
        Colaborador ponente = new Colaborador();
        ponente.setTipoColaborador(RolUsuario.PONENTE);
        when(salaDAO.findById(1L)).thenReturn(Optional.of(sala));
        when(actividadDAO.findBySalaAndFecha(sala, form.getFecha())).thenReturn(List.of());
        when(colaboradorDAO.findById(2L)).thenReturn(Optional.of(ponente));

        servicio.crearDesdeFormulario(form);

        ArgumentCaptor<Actividad> captor = ArgumentCaptor.forClass(Actividad.class);
        verify(actividadDAO).save(captor.capture());
        Ponencia actividad = assertInstanceOf(Ponencia.class, captor.getValue());
        assertEquals("Ciudad inteligente", actividad.getTitulo());
        assertEquals(sala, actividad.getSala());
        assertEquals(ponente, actividad.getPonente());
    }

    @Test
    void rechazaCupoMayorQueLaCapacidad() {
        ActividadFormulario form = formulario();
        when(salaDAO.findById(1L)).thenReturn(Optional.of(new Sala("Sala A", 10, "Piso 1")));

        assertThrows(IllegalArgumentException.class, () -> servicio.crearDesdeFormulario(form));
        verify(actividadDAO, never()).save(any());
    }

    private ActividadFormulario formulario() {
        ActividadFormulario form = new ActividadFormulario();
        form.setTipo("PONENCIA");
        form.setTitulo("Ciudad inteligente");
        form.setFecha(LocalDate.of(2026, 11, 20));
        form.setHorarioInicio(LocalTime.of(9, 0));
        form.setHorarioFin(LocalTime.of(10, 0));
        form.setCupo(20);
        form.setSalaId(1L);
        form.setColaboradorId(2L);
        return form;
    }
}
