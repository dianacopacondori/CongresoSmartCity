package com.congreso.servicio;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.EncriptadorPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServicioTest {

    @Mock
    private UsuarioDAO usuarioDAO;

    // Eliminamos @InjectMocks y declaramos el servicio sin anotaciones
    private UsuarioServicio usuarioServicio;
    private EncriptadorPassword encriptadorPassword;

    private Asistente asistentePrueba;

    @BeforeEach
    void setUp() {
        // 1. Creamos la instancia REAL del encriptador
        encriptadorPassword = new EncriptadorPassword();

        // 2. Inyectamos manualmente las dependencias en el constructor del servicio
        // Esto evita que Mockito pase "null" y cause el NullPointerException
        usuarioServicio = new UsuarioServicio(usuarioDAO, encriptadorPassword);

        // 3. Configuramos el objeto de prueba
        asistentePrueba = new Asistente(
                "Juan", "Pérez", "12345678",
                "juan@example.com", "987654321",
                "password123",
                "Lima", "Estudiante", "Universidad Tecnológica", "Ninguna", 25,
                ModalidadPago.COMPLETO
        );
        asistentePrueba.setCodigo(1L);
    }

    @Test
    void testRegistrarUsuario_Exitoso() {
        // Arrange
        when(usuarioDAO.existsByCorreo("juan@example.com")).thenReturn(false);
        when(usuarioDAO.existsByIdentificacion("12345678")).thenReturn(false);
        when(usuarioDAO.save(any(Asistente.class))).thenReturn(asistentePrueba);

        // Act
        Usuario resultado = usuarioServicio.registrarUsuario(asistentePrueba);

        // Assert
        assertNotNull(resultado);
        assertEquals("juan@example.com", resultado.getCorreo());
        assertEquals(EstadoUsuario.ACTIVO, resultado.getEstado());

        // Verify
        verify(usuarioDAO, times(1)).save(any(Asistente.class));
    }

    @Test
    void testRegistrarUsuario_CorreoExistente() {
        // Arrange
        when(usuarioDAO.existsByCorreo("juan@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioServicio.registrarUsuario(asistentePrueba)
        );

        assertEquals("El correo electrónico ya está registrado", exception.getMessage());
        verify(usuarioDAO, never()).save(any(Asistente.class));
    }

    @Test
    void testRegistrarUsuario_IdentificacionExistente() {
        // Arrange
        when(usuarioDAO.existsByCorreo("juan@example.com")).thenReturn(false);
        when(usuarioDAO.existsByIdentificacion("12345678")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioServicio.registrarUsuario(asistentePrueba)
        );

        assertEquals("La identificación ya está registrada", exception.getMessage());
        verify(usuarioDAO, never()).save(any(Asistente.class));
    }

    @Test
    void testAutenticar_Exitoso() {
        // Arrange: Encriptamos la contraseña con la clase REAL para que coincida
        String contrasenaEncriptada = encriptadorPassword.encriptar("password123");
        asistentePrueba.setContrasena(contrasenaEncriptada);

        when(usuarioDAO.findByCorreo("juan@example.com")).thenReturn(Optional.of(asistentePrueba));

        // Act
        Optional<Usuario> resultado = usuarioServicio.autenticar("juan@example.com", "password123");

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals("juan@example.com", resultado.get().getCorreo());
    }

    @Test
    void testAutenticar_CredencialesIncorrectas() {
        // Arrange
        String contrasenaEncriptada = encriptadorPassword.encriptar("password123");
        asistentePrueba.setContrasena(contrasenaEncriptada);
        when(usuarioDAO.findByCorreo("juan@example.com")).thenReturn(Optional.of(asistentePrueba));

        // Act
        Optional<Usuario> resultado = usuarioServicio.autenticar("juan@example.com", "contrasena_erronea");

        // Assert
        assertFalse(resultado.isPresent());
    }

    @Test
    void testAutenticar_UsuarioSuspendido() {
        // Arrange
        asistentePrueba.setEstado(EstadoUsuario.SUSPENDIDO);
        when(usuarioDAO.findByCorreo("juan@example.com")).thenReturn(Optional.of(asistentePrueba));

        // Act
        Optional<Usuario> resultado = usuarioServicio.autenticar("juan@example.com", "password123");

        // Assert
        assertFalse(resultado.isPresent());
    }

    @Test
    void testSuspenderUsuario_Exitoso() {
        // Arrange
        when(usuarioDAO.findById(1L)).thenReturn(Optional.of(asistentePrueba));
        when(usuarioDAO.save(any(Usuario.class))).thenReturn(asistentePrueba);

        // Act
        Usuario resultado = usuarioServicio.suspenderUsuario(1L);

        // Assert
        assertNotNull(resultado);
        assertEquals(EstadoUsuario.SUSPENDIDO, resultado.getEstado());
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