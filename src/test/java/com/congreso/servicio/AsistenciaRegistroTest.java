package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenciaDAO;
import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Asistencia;
import com.congreso.modelo.Fotocheck;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.modelo.enums.EstadoAsistencia;
import com.congreso.modelo.enums.EstadoFotocheck;
import com.congreso.modelo.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsistenciaRegistroTest {

    private static final String ADMIN = "admin@example.invalid";

    private AsistenciaDAO asistenciaDAO;
    private AsistenteDAO asistenteDAO;
    private ActividadDAO actividadDAO;
    private InscripcionServicio inscripcionServicio;
    private FotocheckServicio fotocheckServicio;
    private UsuarioDAO usuarioDAO;
    private AsistenciaServicio servicio;
    private Asistente asistente;
    private Actividad actividad;
    private Inscripcion inscripcion;
    private Fotocheck fotocheck;

    @BeforeEach
    void configurar() {
        asistenciaDAO = mock(AsistenciaDAO.class);
        asistenteDAO = mock(AsistenteDAO.class);
        actividadDAO = mock(ActividadDAO.class);
        inscripcionServicio = mock(InscripcionServicio.class);
        fotocheckServicio = mock(FotocheckServicio.class);
        usuarioDAO = mock(UsuarioDAO.class);
        servicio = new AsistenciaServicio(asistenciaDAO, asistenteDAO, actividadDAO,
                inscripcionServicio, fotocheckServicio, usuarioDAO);

        asistente = new Asistente();
        asistente.setCodigo(1L);
        asistente.setCorreo("ana@example.invalid");
        asistente.setRol(RolUsuario.ASISTENTE);
        actividad = new Ponencia();
        actividad.setCodigo(2L);
        actividad.setEstado(EstadoActividad.ACTIVA);
        actividad.setFecha(LocalDate.of(2026, 11, 22));
        inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.getActividades().add(actividad);
        fotocheck = new Fotocheck(asistente);
        fotocheck.setCodigo(3L);
        Administrador admin = new Administrador();
        admin.setCorreo(ADMIN);
        admin.setRol(RolUsuario.ADMINISTRADOR);

        when(asistenteDAO.findById(1L)).thenReturn(Optional.of(asistente));
        when(actividadDAO.findById(2L)).thenReturn(Optional.of(actividad));
        when(inscripcionServicio.inscripcionDisponible(asistente.getCorreo())).thenReturn(Optional.of(inscripcion));
        when(usuarioDAO.findByCorreo(ADMIN)).thenReturn(Optional.of(admin));
        when(fotocheckServicio.buscarPorCodigo(3L)).thenReturn(Optional.of(fotocheck));
        when(fotocheckServicio.validarFotocheck(3L)).thenReturn(true);
        when(asistenciaDAO.findByAsistenteAndActividad(asistente, actividad)).thenReturn(Optional.empty());
        when(asistenciaDAO.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void codigoValidoRegistraAsistenciaConFechaDeLaActividadYRegistrador() {
        Asistencia registrada = servicio.registrarAsistencia(1L, 2L, "codigo", 3L, ADMIN);

        assertEquals(EstadoAsistencia.PRESENTE, registrada.getEstado());
        assertEquals("CODIGO", registrada.getMecanismoValidacion());
        assertEquals(actividad.getFecha(), registrada.getFecha());
        assertEquals(3L, fotocheck.getCodigo());
        assertEquals(ADMIN, registrada.getRegistradoPor().getCorreo());
        verify(asistenciaDAO).saveAndFlush(registrada);
    }

    @Test
    void registroManualRequiereAdministradorIdentificado() {
        Asistencia registrada = servicio.registrarAsistencia(1L, 2L, "MANUAL", null, ADMIN);

        assertEquals("MANUAL", registrada.getMecanismoValidacion());
        assertEquals(ADMIN, registrada.getRegistradoPor().getCorreo());

        assertThrows(IllegalStateException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "MANUAL", null, null));
    }

    @Test
    void sinInscripcionElegibleNoRegistra() {
        when(inscripcionServicio.inscripcionDisponible(asistente.getCorreo())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "MANUAL", null, ADMIN));
        verify(asistenciaDAO, never()).saveAndFlush(any());
    }

    @Test
    void actividadFueraDelAgendaNoRegistra() {
        inscripcion.getActividades().clear();

        assertThrows(IllegalStateException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "MANUAL", null, ADMIN));
        verify(asistenciaDAO, never()).saveAndFlush(any());
    }

    @Test
    void credencialInvalidaNoRegistra() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "QR", null, ADMIN));
        when(fotocheckServicio.validarFotocheck(3L)).thenReturn(false);
        assertThrows(IllegalStateException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "QR", 3L, ADMIN));
        verify(asistenciaDAO, never()).saveAndFlush(any());
    }

    @Test
    void fotocheckDeOtroAsistenteNoRegistra() {
        Asistente otro = new Asistente();
        otro.setCodigo(8L);
        Fotocheck ajeno = new Fotocheck(otro);
        ajeno.setCodigo(4L);
        when(fotocheckServicio.buscarPorCodigo(4L)).thenReturn(Optional.of(ajeno));
        when(fotocheckServicio.validarFotocheck(4L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "CODIGO", 4L, ADMIN));
        verify(asistenciaDAO, never()).saveAndFlush(any());
    }

    @Test
    void mecanismoDesconocidoYDobleRegistroSeRechazan() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "HUELLA", null, ADMIN));

        when(asistenciaDAO.findByAsistenteAndActividad(asistente, actividad))
                .thenReturn(Optional.of(new Asistencia()));
        assertThrows(IllegalStateException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "MANUAL", null, ADMIN));
        verify(asistenciaDAO, never()).saveAndFlush(any());
    }

    @Test
    void correccionGuardaJustificacionCorrectorYFecha() {
        Asistencia existente = new Asistencia();
        existente.setCodigo(20L);
        existente.setEstado(EstadoAsistencia.PRESENTE);
        when(asistenciaDAO.findById(20L)).thenReturn(Optional.of(existente));

        servicio.corregirAsistencia(20L, "  Se validó fotocheck manualmente  ", ADMIN);

        assertEquals(EstadoAsistencia.CORREGIDA, existente.getEstado());
        assertEquals("Se validó fotocheck manualmente", existente.getJustificacionCorreccion());
        assertEquals(ADMIN, existente.getCorregidoPor().getCorreo());
        assertEquals(java.time.LocalDate.now(), existente.getFechaCorreccion().toLocalDate());
        verify(asistenciaDAO).saveAndFlush(existente);
    }

    @Test
    void correccionRequiereAdministradorYJustificacion() {
        Asistencia existente = new Asistencia();
        existente.setCodigo(21L);
        when(asistenciaDAO.findById(21L)).thenReturn(Optional.of(existente));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> servicio.corregirAsistencia(21L, "justificacion valida", null));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.corregirAsistencia(21L, "abc", ADMIN));
        verify(asistenciaDAO, never()).saveAndFlush(any());
    }

    @Test
    void violacionDeUnicidadEnBaseSeTraduceEnErrorComprensible() {
        when(asistenciaDAO.saveAndFlush(any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_asistencia"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "MANUAL", null, ADMIN));
        assertTrue(error.getMessage().contains("ya fue registrada"));
    }

    @Test
    void actividadCanceladaOSinFechaSeRechaza() {
        actividad.setEstado(EstadoActividad.CANCELADA);
        assertThrows(IllegalStateException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "MANUAL", null, ADMIN));

        actividad.setEstado(EstadoActividad.ACTIVA);
        actividad.setFecha(null);
        assertThrows(IllegalStateException.class,
                () -> servicio.registrarAsistencia(1L, 2L, "MANUAL", null, ADMIN));
        verify(asistenciaDAO, never()).saveAndFlush(any());
    }
}
