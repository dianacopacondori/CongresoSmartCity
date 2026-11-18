package com.congreso.seguridad;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.RolUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfiguracionSeguridadTest {

    @Test
    void rechazaAdministradorGuardadoConRolAsistente() {
        UsuarioDAO dao = mock(UsuarioDAO.class);
        Administrador administrador = new Administrador();
        administrador.setCorreo("admin@example.com");
        administrador.setRol(RolUsuario.ASISTENTE);
        when(dao.findByCorreo("admin@example.com")).thenReturn(Optional.of(administrador));

        assertThrows(UsernameNotFoundException.class,
                () -> new ConfiguracionSeguridad().userDetailsService(dao).loadUserByUsername("admin@example.com"));
    }

    @Test
    void rechazaAsistenteGuardadoConRolAdministrador() {
        UsuarioDAO dao = mock(UsuarioDAO.class);
        Asistente asistente = new Asistente();
        asistente.setCorreo("asistente@example.com");
        asistente.setRol(RolUsuario.ADMINISTRADOR);
        when(dao.findByCorreo("asistente@example.com")).thenReturn(Optional.of(asistente));

        assertThrows(UsernameNotFoundException.class,
                () -> new ConfiguracionSeguridad().userDetailsService(dao)
                        .loadUserByUsername("asistente@example.com"));
    }

    @Test
    void elAdministradorValidoObtieneSuRolYRestaActivo() {
        UsuarioDAO dao = mock(UsuarioDAO.class);
        Administrador administrador = new Administrador();
        administrador.setCorreo("admin@example.com");
        administrador.setContrasena("$2a$10$hash");
        administrador.setRol(RolUsuario.ADMINISTRADOR);
        administrador.setEstado(EstadoUsuario.ACTIVO);
        when(dao.findByCorreo("admin@example.com")).thenReturn(Optional.of(administrador));

        UserDetails detalles = new ConfiguracionSeguridad().userDetailsService(dao)
                .loadUserByUsername("admin@example.com");

        assertEquals("admin@example.com", detalles.getUsername());
        assertTrue(detalles.getAuthorities().stream()
                .anyMatch(autoridad -> autoridad.getAuthority().equals("ROLE_ADMINISTRADOR")));
        assertTrue(detalles.isEnabled());
    }

    @Test
    void unUsuarioInactivoSeCargaDeshabilitado() {
        UsuarioDAO dao = mock(UsuarioDAO.class);
        Asistente asistente = new Asistente();
        asistente.setCorreo("inactivo@example.com");
        asistente.setContrasena("$2a$10$hash");
        asistente.setRol(RolUsuario.ASISTENTE);
        asistente.setEstado(EstadoUsuario.SUSPENDIDO);
        when(dao.findByCorreo("inactivo@example.com")).thenReturn(Optional.of(asistente));

        UserDetails detalles = new ConfiguracionSeguridad().userDetailsService(dao)
                .loadUserByUsername("inactivo@example.com");

        assertFalse(detalles.isEnabled());
    }

    @Test
    void elCodificadorDeContrasenasEsBCrypt() {
        assertTrue(new ConfiguracionSeguridad().passwordEncoder().encode("clave").startsWith("$2"));
    }
}
