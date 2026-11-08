package com.congreso.seguridad;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.enums.RolUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
