package com.congreso.servicio;

import com.congreso.dao.FotocheckDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Fotocheck;
import com.congreso.modelo.enums.EstadoFotocheck;
import com.congreso.modelo.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FotocheckServicioTest {

    private FotocheckDAO dao;
    private FotocheckServicio servicio;
    private Asistente asistente;

    @BeforeEach
    void configurar() {
        dao = mock(FotocheckDAO.class);
        servicio = new FotocheckServicio(dao);
        asistente = new Asistente();
        asistente.setCodigo(1L);
        asistente.setCorreo("ana@example.invalid");
        asistente.setNombres("Ana");
        asistente.setApellidos("Pérez");
        asistente.setRol(RolUsuario.ASISTENTE);
        when(dao.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void emiteUnFotocheckNuevo() {
        when(dao.findByUsuario(asistente)).thenReturn(Optional.empty());

        Fotocheck emitido = servicio.generarFotocheck(asistente);

        assertEquals(EstadoFotocheck.EMITIDO, emitido.getEstado());
        assertEquals("Ana", emitido.getNombres());
        assertSame(asistente, emitido.getUsuario());
        verify(dao).save(emitido);
    }

    @Test
    void noDuplicaUnFotocheckYaEmitido() {
        Fotocheck existente = new Fotocheck(asistente);
        when(dao.findByUsuario(asistente)).thenReturn(Optional.of(existente));

        Fotocheck resultado = servicio.generarFotocheck(asistente);

        assertSame(existente, resultado);
        verify(dao, never()).save(any());
    }

    @Test
    void renuevaElMismoRegistroTrasAnulacionSinCrearOtro() {
        Fotocheck anulado = new Fotocheck(asistente);
        anulado.setCodigo(9L);
        anulado.setEstado(EstadoFotocheck.ANULADO);
        anulado.setFechaEmision(LocalDate.of(2020, 1, 1));
        when(dao.findByUsuario(asistente)).thenReturn(Optional.of(anulado));

        Fotocheck renovado = servicio.generarFotocheck(asistente);

        assertSame(anulado, renovado);
        assertEquals(9L, renovado.getCodigo());
        assertEquals(EstadoFotocheck.EMITIDO, renovado.getEstado());
        assertEquals(LocalDate.now(), renovado.getFechaEmision());
        verify(dao).save(anulado);
    }

    @Test
    void anulaYValidaPorCodigo() {
        Fotocheck emitido = new Fotocheck(asistente);
        emitido.setCodigo(5L);
        when(dao.findById(5L)).thenReturn(Optional.of(emitido));
        when(dao.findByCodigo(5L)).thenReturn(Optional.of(emitido));

        assertTrue(servicio.validarFotocheck(5L));
        servicio.anularFotocheck(5L);

        assertEquals(EstadoFotocheck.ANULADO, emitido.getEstado());
        assertFalse(servicio.validarFotocheck(5L));
    }
}
