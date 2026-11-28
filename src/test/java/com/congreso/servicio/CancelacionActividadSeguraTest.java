package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.ColaboradorDAO;
import com.congreso.dao.SalaDAO;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.enums.EstadoActividad;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CancelacionActividadSeguraTest {
    private final ActividadDAO dao = mock(ActividadDAO.class);
    private final ActividadServicio servicio = new ActividadServicio(
            dao, mock(SalaDAO.class), mock(ColaboradorDAO.class));

    @Test
    void noCancelaUnaActividadConAsistentesInscritos() {
        Ponencia actividad = new Ponencia();
        actividad.setInscritos(2);
        when(dao.findById(9L)).thenReturn(Optional.of(actividad));

        assertThrows(IllegalStateException.class, () -> servicio.cancelarActividad(9L));
        assertEquals(EstadoActividad.ACTIVA, actividad.getEstado());
        verify(dao, never()).save(any());
    }

    @Test
    void permiteCancelarUnaActividadSinInscritos() {
        Ponencia actividad = new Ponencia();
        actividad.setInscritos(0);
        when(dao.findById(9L)).thenReturn(Optional.of(actividad));

        servicio.cancelarActividad(9L);
        assertEquals(EstadoActividad.CANCELADA, actividad.getEstado());
        verify(dao).save(actividad);
    }
}
