package com.congreso.servicio;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dto.PerfilAsistenteFormulario;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.modelo.enums.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class AsistentePerfilServicioTest {
    private AsistenteDAO dao;
    private AsistenteServicio servicio;

    @BeforeEach
    void setUp() {
        dao = mock(AsistenteDAO.class);
        servicio = new AsistenteServicio(dao);
    }

    @Test
    void actualizaSoloLosCamposPermitidosDelUsuarioAutenticado() {
        Asistente asistente = asistente("ana@example.com");
        asistente.setCodigo(7L);
        asistente.setIdentificacion("12345678");
        asistente.setContrasena("hash-almacenado");
        when(dao.findByCorreo("ana@example.com")).thenReturn(Optional.of(asistente));

        PerfilAsistenteFormulario form = new PerfilAsistenteFormulario();
        form.setNombres(" Ana María ");
        form.setApellidos(" Torres ");
        form.setTelefono("999888777");
        form.setEdad(22);
        form.setModalidadPagoPreferida(ModalidadPago.CUOTAS);

        servicio.actualizarPerfilPropio("ana@example.com", form);

        verify(dao).save(asistente);
        assertEquals("Ana María", asistente.getNombres());
        assertEquals("Torres", asistente.getApellidos());
        assertEquals("999888777", asistente.getTelefono());
        assertEquals(22, asistente.getEdad());
        assertEquals(ModalidadPago.CUOTAS, asistente.getModalidadPagoPreferida());
        assertEquals(7L, asistente.getCodigo());
        assertEquals("12345678", asistente.getIdentificacion());
        assertEquals("ana@example.com", asistente.getCorreo());
        assertEquals("hash-almacenado", asistente.getContrasena());
        assertEquals(RolUsuario.ASISTENTE, asistente.getRol());
        assertEquals(EstadoUsuario.ACTIVO, asistente.getEstado());
    }

    @Test
    void noPuedeActualizarSiElCorreoAutenticadoNoEsElDeLaCuentaBuscada() {
        when(dao.findByCorreo("otro@example.com")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class,
                () -> servicio.actualizarPerfilPropio("otro@example.com", new PerfilAsistenteFormulario()));
        verify(dao, never()).save(any());
    }

    @Test
    void noPermiteActualizarUnAsistenteSuspendido() {
        Asistente asistente = asistente("ana@example.com");
        asistente.setEstado(EstadoUsuario.SUSPENDIDO);
        when(dao.findByCorreo("ana@example.com")).thenReturn(Optional.of(asistente));
        assertThrows(AccessDeniedException.class,
                () -> servicio.actualizarPerfilPropio("ana@example.com", new PerfilAsistenteFormulario()));
        verify(dao, never()).save(any());
    }

    @Test
    void formularioSeConstruyeSinCredencialesNiCamposAdministrativos() {
        Asistente asistente = asistente("ana@example.com");
        asistente.setNombres("Ana");
        PerfilAsistenteFormulario form = PerfilAsistenteFormulario.desde(asistente);
        assertEquals("Ana", form.getNombres());
        assertEquals(PerfilAsistenteFormulario.class, form.getClass());
    }

    private Asistente asistente(String correo) {
        Asistente asistente = new Asistente();
        asistente.setCorreo(correo);
        asistente.setRol(RolUsuario.ASISTENTE);
        asistente.setEstado(EstadoUsuario.ACTIVO);
        return asistente;
    }
}
