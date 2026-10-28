package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.AsistenciaDAO;
import com.congreso.modelo.Asistencia;
import com.congreso.modelo.enums.EstadoAsistencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaServicioTest {

    @Mock
    private AsistenciaDAO asistenciaDAO;

    @Mock
    private AsistenteDAO asistenteDAO;

    @Mock
    private ActividadDAO actividadDAO;

    private AsistenciaServicio asistenciaServicio;

    @BeforeEach
    void setUp() {
        asistenciaServicio = new AsistenciaServicio(asistenciaDAO, asistenteDAO, actividadDAO);
    }

    @Test
    void corregirAsistenciaActualizaEstadoYGuarda() {
        Asistencia asistencia = new Asistencia();
        asistencia.setCodigo(15L);
        asistencia.setEstado(EstadoAsistencia.PRESENTE);
        when(asistenciaDAO.findById(15L)).thenReturn(Optional.of(asistencia));
        when(asistenciaDAO.guardarConRegistro(asistencia)).thenReturn(asistencia);

        Asistencia resultado = asistenciaServicio.corregirAsistencia(15L, EstadoAsistencia.AUSENTE);

        assertEquals(EstadoAsistencia.AUSENTE, resultado.getEstado());
        verify(asistenciaDAO).guardarConRegistro(asistencia);
    }

    @Test
    void corregirAsistenciaRechazaEstadoCorregida() {
        assertThrows(IllegalArgumentException.class,
                () -> asistenciaServicio.corregirAsistencia(15L, EstadoAsistencia.CORREGIDA));

        verify(asistenciaDAO, never()).findById(15L);
    }
}
