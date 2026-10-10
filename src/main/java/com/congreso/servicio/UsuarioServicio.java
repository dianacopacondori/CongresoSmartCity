package com.congreso.servicio;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.EncriptadorPassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de usuarios.
 * Aplica el principio de responsabilidad única (SRP) de SOLID.
 */
@Service
@Transactional
public class UsuarioServicio {

    private final UsuarioDAO usuarioDAO;
    private final EncriptadorPassword encriptadorPassword;

    @Autowired
    public UsuarioServicio(UsuarioDAO usuarioDAO, EncriptadorPassword encriptadorPassword) {
        this.usuarioDAO = usuarioDAO;
        this.encriptadorPassword = encriptadorPassword;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param usuario el usuario a registrar
     * @return el usuario registrado
     * @throws IllegalArgumentException si el correo o identificación ya existen
     */
    public Usuario registrarUsuario(Usuario usuario) {
        // Validar que el correo no exista
        if (usuarioDAO.existsByCorreo(usuario.getCorreo())) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado");
        }

        // Validar que la identificación no exista
        if (usuarioDAO.existsByIdentificacion(usuario.getIdentificacion())) {
            throw new IllegalArgumentException("La identificación ya está registrada");
        }

        // Encriptar la contraseña
        usuario.setContrasena(encriptadorPassword.encriptar(usuario.getContrasena()));

        // Establecer estado activo por defecto
        usuario.setEstado(EstadoUsuario.ACTIVO);

        // Guardar el usuario
        return usuarioDAO.save(usuario);
    }

    /**
     * Autentica un usuario con sus credenciales.
     *
     * @param correo el correo electrónico del usuario
     * @param contrasena la contraseña del usuario
     * @return un Optional con el usuario autenticado, o vacío si las credenciales son incorrectas
     */
    public Optional<Usuario> autenticar(String correo, String contrasena) {
        Optional<Usuario> usuarioOpt = usuarioDAO.findByCorreo(correo);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // Verificar que el usuario esté activo
            if (!usuario.estaActivo()) {
                return Optional.empty();
            }

            // Verificar la contraseña
            if (encriptadorPassword.verificar(contrasena, usuario.getContrasena())) {
                return Optional.of(usuario);
            }
        }

        return Optional.empty();
    }

    /**
     * Actualiza los datos de un usuario existente.
     *
     * @param usuario el usuario con los datos actualizados
     * @return el usuario actualizado
     * @throws IllegalArgumentException si el usuario no existe
     */
    public Usuario actualizarUsuario(Usuario usuario) {
        if (!usuarioDAO.existsById(usuario.getCodigo())) {
            throw new IllegalArgumentException("El usuario no existe");
        }

        return usuarioDAO.save(usuario);
    }

    /**
     * Suspender un usuario.
     *
     * @param codigoUsuario el código del usuario a suspender
     * @return el usuario suspendido
     * @throws IllegalArgumentException si el usuario no existe
     */
    public Usuario suspenderUsuario(Long codigoUsuario) {
        Usuario usuario = usuarioDAO.findById(codigoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));

        usuario.setEstado(EstadoUsuario.SUSPENDIDO);
        return usuarioDAO.save(usuario);
    }

    /**
     * Elimina lógicamente un usuario (cambia su estado a ELIMINADO).
     *
     * @param codigoUsuario el código del usuario a eliminar
     * @return el usuario eliminado
     * @throws IllegalArgumentException si el usuario no existe
     */
    public Usuario eliminarUsuario(Long codigoUsuario) {
        Usuario usuario = usuarioDAO.findById(codigoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe"));

        usuario.setEstado(EstadoUsuario.ELIMINADO);
        return usuarioDAO.save(usuario);
    }

    /**
     * Busca un usuario por su código.
     *
     * @param codigo el código del usuario
     * @return un Optional con el usuario encontrado, o vacío si no existe
     */
    public Optional<Usuario> buscarPorCodigo(Long codigo) {
        return usuarioDAO.findById(codigo);
    }

    /**
     * Busca un usuario por su correo electrónico.
     *
     * @param correo el correo electrónico del usuario
     * @return un Optional con el usuario encontrado, o vacío si no existe
     */
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioDAO.findByCorreo(correo);
    }

    /**
     * Obtiene todos los usuarios con un rol específico.
     *
     * @param rol el rol a buscar
     * @return una lista de usuarios con el rol especificado
     */
    public List<Usuario> buscarPorRol(RolUsuario rol) {
        return usuarioDAO.findByRol(rol);
    }

    /**
     * Busca usuarios por nombre o apellido.
     *
     * @param nombre el nombre o apellido a buscar
     * @return una lista de usuarios que coinciden con la búsqueda
     */
    public List<Usuario> buscarPorNombre(String nombre) {
        return usuarioDAO.buscarPorNombre(nombre);
    }

    /**
     * Obtiene todos los usuarios activos.
     *
     * @return una lista de usuarios activos
     */
    public List<Usuario> obtenerUsuariosActivos() {
        return usuarioDAO.findByEstado(EstadoUsuario.ACTIVO);
    }
}
