package com.congreso.seguridad;

import com.congreso.modelo.Usuario;
import com.congreso.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Componente para la autenticación de usuarios.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Component
public class Autenticador {

    private static final Logger log = LoggerFactory.getLogger(Autenticador.class);

    private final UsuarioServicio usuarioServicio;

    @Autowired
    public Autenticador(UsuarioServicio usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    /**
     * Autentica un usuario con sus credenciales.
     *
     * @param correo el correo electrónico del usuario
     * @param contrasena la contraseña del usuario
     * @return un Optional con el usuario autenticado, o vacío si las credenciales son incorrectas
     */
    public Optional<Usuario> autenticar(String correo, String contrasena) {
        Optional<Usuario> usuario = usuarioServicio.autenticar(correo, contrasena);
        if (usuario.isEmpty()) {
            log.warn("Autenticación fallida: credenciales inválidas o usuario inactivo");
        }
        return usuario;
    }

    /**
     * Verifica si un usuario tiene un rol específico.
     *
     * @param usuario el usuario a verificar
     * @param rol el rol a verificar
     * @return true si el usuario tiene el rol especificado, false en caso contrario
     */
    public boolean tieneRol(Usuario usuario, com.congreso.modelo.enums.RolUsuario rol) {
        return usuario.getRol() == rol;
    }

    /**
     * Verifica si un usuario es administrador.
     *
     * @param usuario el usuario a verificar
     * @return true si el usuario es administrador, false en caso contrario
     */
    public boolean esAdministrador(Usuario usuario) {
        return tieneRol(usuario, com.congreso.modelo.enums.RolUsuario.ADMINISTRADOR);
    }
}