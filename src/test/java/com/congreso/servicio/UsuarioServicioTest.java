package com.congreso.servicio;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.EncriptadorPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para UsuarioServicio.
 * Aplica el enfoque TDD (Test-Driven Development).
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServicioTest {

    @Mock
    private UsuarioDAO usuarioDAO;

    @Mock
    private EncriptadorPassword encriptadorPassword;

    @InjectMocks
    private UsuarioServicio usuarioServicio;

    private Usuario usuarioPrueba;

    @BeforeEach
    void setUp() {
        usuarioPrueba = new Usuario(
                "Juan", "Pérez", "12345678",
                "juan@example.com", "987654321",
                "password123", RolUsuario.ASISTENTE
        );
        usuarioPrueba.setCodigo(1L);
    }

    @Test
    void testRegistrarUsuario_Exitoso() {
        // Arrange
        when(usuarioDAO.existsByCorreo("juan@example.com")).thenReturn(false);
        when(usuarioDAO.existsByIdentificacion("12345678")).thenReturn(false);
        when(encriptadorPassword.encriptar("password123")).thenReturn("encrypted_password");
        when(usuarioDAO.save(any(Usuario.class))).thenReturn(usuarioPrueba);

        // Act
        Usuario resultado = usuarioServicio.registrarUsuario(usuarioPrueba);

        // Assert
        assertNotNull(resultado);
        assertEquals("juan@example.com", resultado.getCorreo());
        assertEquals("encrypted_password", resultado.getContrasena());
        assertEquals(EstadoUsuario.ACTIVO, resultado.getEstado());

        // Verify
        verify(usuarioDAO, times(1)).save(any(Usuario.class));
    }

    @Test
    void testRegistrarUsuario_CorreoExistente() {
        // Arrange
        when(usuarioDAO.existsByCorreo("juan@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioServicio.registrarUsuario(usuarioPrueba)
        );

        assertEquals("El correo electrónico ya está registrado", exception.getMessage());
        verify(usuarioDAO, never()).save(any(Usuario.class));
    }

    @Test
    void testRegistrarUsuario_IdentificacionExistente() {
        // Arrange
        when(usuarioDAO.existsByCorreo("juan@example.com")).thenReturn(false);
        when(usuarioDAO.existsByIdentificacion("12345678")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioServicio.registrarUsuario(usuarioPrueba)
        );

        assertEquals("La identificación ya está registrada", exception.getMessage());
        verify(usuarioDAO, never()).save(any(Usuario.class));
    }

    @Test
    void testAutenticar_Exitoso() {
        // Arrange
        when(usuarioDAO.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuarioPrueba));
        when(encriptadorPassword.verificar("password123", "encrypted_password")).thenReturn(true);
        usuarioPrueba.setContrasena("encrypted_password");

        // Act
        Optional<Usuario> resultado = usuarioServicio.autenticar("juan@example.com", "password123");

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals("juan@example.com", resultado.get().getCorreo());
    }

    @Test
    void testAutenticar_CredencialesIncorrectas() {
        // Arrange
        when(usuarioDAO.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuarioPrueba));
        when(encriptadorPassword.verificar("password_incorrecto", "encrypted_password")).thenReturn(false);
        usuarioPrueba.setContrasena("encrypted_password");

        // Act
        Optional<Usuario> resultado = usuarioServicio.autenticar("juan@example.com", "password_incorrecto");

        // Assert
        assertFalse(resultado.isPresent());
    }

    @Test
    void testAutenticar_UsuarioSuspendido() {
        // Arrange
        usuarioPrueba.setEstado(EstadoUsuario.SUSPENDIDO);
        when(usuarioDAO.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuarioPrueba));

        // Act
        Optional<Usuario> resultado = usuarioServicio.autenticar("juan@example.com", "password123");

        // Assert
        assertFalse(resultado.isPresent());
    }

    @Test
    void testSuspenderUsuario_Exitoso() {
        // Arrange
        when(usuarioDAO.findById(1L)).thenReturn(Optional.of(usuarioPrueba));
        when(usuarioDAO.save(any(Usuario.class))).thenReturn(usuarioPrueba);

        // Act
        Usuario resultado = usuarioServicio.suspenderUsuario(1L);

        // Assert
        assertNotNull(resultado);
        assertEquals(EstadoUsuario.SUSPENDIDO, resultado.getEstado());

        // Verify
        verify(usuarioDAO, times(1)).save(any(Usuario.class));
    }

    @Test
    void testSuspenderUsuario_NoExiste() {
        // Arrange
        when(usuarioDAO.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioServicio.suspenderUsuario(999L)
        );

        assertEquals("El usuario no existe", exception.getMessage());
    }
}