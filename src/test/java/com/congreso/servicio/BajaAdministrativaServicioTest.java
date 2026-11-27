package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.EstadoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BajaAdministrativaServicioTest {

    private AsistenteDAO asistenteDAO;
    private InscripcionDAO inscripcionDAO;
    private ActividadDAO actividadDAO;
    private PagoDAO pagoDAO;
    private AsistenteServicio asistenteServicio;
    private InscripcionServicio inscripcionServicio;

    @BeforeEach
    void configurar() {
        asistenteDAO = mock(AsistenteDAO.class);
        inscripcionDAO = mock(InscripcionDAO.class);
        actividadDAO = mock(ActividadDAO.class);
        pagoDAO = mock(PagoDAO.class);
        asistenteServicio = new AsistenteServicio(asistenteDAO);
        inscripcionServicio = new InscripcionServicio(inscripcionDAO, asistenteDAO, actividadDAO, pagoDAO);
    }

    @Test
    void bajaDeAsistenteEsLogicaYNoBorraLaFila() {
        Asistente asistente = new Asistente();
        asistente.setEstado(EstadoUsuario.ACTIVO);
        when(asistenteDAO.findById(7L)).thenReturn(Optional.of(asistente));

        asistenteServicio.eliminarAsistente(7L, "admin@example.invalid");

        assertEquals(EstadoUsuario.ELIMINADO, asistente.getEstado());
        verify(asistenteDAO).save(asistente);
        verify(asistenteDAO, never()).deleteById(7L);
    }

    @Test
    void suspensionNoPermiteReactivarUnAsistenteDadoDeBaja() {
        Asistente asistente = new Asistente();
        asistente.setEstado(EstadoUsuario.ELIMINADO);
        when(asistenteDAO.findById(8L)).thenReturn(Optional.of(asistente));

        assertThrows(IllegalStateException.class,
                () -> asistenteServicio.suspenderAsistente(8L, "admin@example.invalid"));
        verify(asistenteDAO, never()).save(asistente);
    }

    @Test
    void cancelarInscripcionLiberaUnCupoPorActividadDistinta() {
        Actividad primera = actividad(1L, 3);
        Actividad segunda = actividad(2L, 1);
        Inscripcion inscripcion = inscripcion(primera, segunda, primera);
        when(inscripcionDAO.bloquearPorCodigo(11L)).thenReturn(Optional.of(inscripcion));
        when(pagoDAO.findByInscripcion(inscripcion)).thenReturn(Optional.empty());
        when(actividadDAO.bloquearCodigo(1L)).thenReturn(Optional.of(1L));
        when(actividadDAO.bloquearCodigo(2L)).thenReturn(Optional.of(2L));
        when(actividadDAO.findById(1L)).thenReturn(Optional.of(primera));
        when(actividadDAO.findById(2L)).thenReturn(Optional.of(segunda));

        inscripcionServicio.cancelarInscripcion(11L, "admin@example.invalid");

        assertEquals(2, primera.getInscritos());
        assertEquals(0, segunda.getInscritos());
        assertEquals(EstadoInscripcion.CANCELADA, inscripcion.getEstado());
        assertTrue(inscripcion.getActividades().isEmpty());
        verify(actividadDAO).save(primera);
        verify(actividadDAO).save(segunda);
        verify(inscripcionDAO).saveAndFlush(inscripcion);
    }

    @Test
    void cancelarInscripcionNoLiberaCupoSiHayImporteCubierto() {
        Actividad actividad = actividad(1L, 3);
        Inscripcion inscripcion = inscripcion(actividad);
        Pago pago = new Pago();
        pago.setEstado(EstadoPago.PAGADO);
        pago.setImporte(50.0);
        pago.setSaldo(0.0);
        when(inscripcionDAO.bloquearPorCodigo(12L)).thenReturn(Optional.of(inscripcion));
        when(pagoDAO.findByInscripcion(inscripcion)).thenReturn(Optional.of(pago));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> inscripcionServicio.cancelarInscripcion(12L, "admin@example.invalid"));

        assertTrue(error.getMessage().contains("política de devolución"));
        assertEquals(3, actividad.getInscritos());
        assertEquals(EstadoInscripcion.CONFIRMADA, inscripcion.getEstado());
        verify(actividadDAO, never()).save(actividad);
        verify(inscripcionDAO, never()).saveAndFlush(inscripcion);
    }

    @Test
    void cancelarInscripcionYaCanceladaSeRechaza() {
        Inscripcion inscripcion = inscripcion();
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        when(inscripcionDAO.bloquearPorCodigo(13L)).thenReturn(Optional.of(inscripcion));

        assertThrows(IllegalStateException.class,
                () -> inscripcionServicio.cancelarInscripcion(13L, "admin@example.invalid"));
        verify(inscripcionDAO, never()).saveAndFlush(inscripcion);
    }

    @Test
    void cancelarInscripcionSinActividadesNoTocaContadores() {
        Inscripcion inscripcion = inscripcion();
        when(inscripcionDAO.bloquearPorCodigo(14L)).thenReturn(Optional.of(inscripcion));
        when(pagoDAO.findByInscripcion(inscripcion)).thenReturn(Optional.empty());

        inscripcionServicio.cancelarInscripcion(14L, "admin@example.invalid");

        assertEquals(EstadoInscripcion.CANCELADA, inscripcion.getEstado());
        verify(actividadDAO, never()).save(org.mockito.ArgumentMatchers.any());
        verify(inscripcionDAO).saveAndFlush(inscripcion);
    }

    private static Inscripcion inscripcion(Actividad... actividades) {
        Inscripcion inscripcion = new Inscripcion(new Asistente(), "Estudiante");
        inscripcion.setCodigo(11L);
        inscripcion.setEstado(EstadoInscripcion.CONFIRMADA);
        inscripcion.getActividades().addAll(List.of(actividades));
        return inscripcion;
    }

    private static Actividad actividad(Long codigo, int inscritos) {
        Actividad actividad = new Ponencia();
        actividad.setCodigo(codigo);
        actividad.setInscritos(inscritos);
        return actividad;
    }
}
