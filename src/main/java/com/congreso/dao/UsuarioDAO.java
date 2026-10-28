package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Usuario.
 * Extiende JpaRepository para obtener operaciones CRUD básicas.
 */
@Repository
public interface UsuarioDAO extends JpaRepository<Usuario, Long>{

    /**
     * Busca un usuario por su correo electrónico.
     *
     * @param correo el correo electrónico del usuario
     * @return un Optional con el usuario encontrado, o vacío si no existe
     */
    Optional<Usuario> findByCorreo(String correo);

    /**
     * Busca un usuario por su identificación (DNI/RUC).
     *
     * @param identificacion la identificación del usuario
     * @return un Optional con el usuario encontrado, o vacío si no existe
     */
    Optional<Usuario> findByIdentificacion(String identificacion);

    /**
     * Busca usuarios por su rol.
     *
     * @param rol el rol del usuario
     * @return una lista de usuarios con el rol especificado
     */
    List<Usuario> findByRol(RolUsuario rol);

    /**
     * Busca usuarios por su estado.
     *
     * @param estado el estado del usuario
     * @return una lista de usuarios con el estado especificado
     */
    List<Usuario> findByEstado(EstadoUsuario estado);

    /**
     * Busca usuarios por nombre o apellido (búsqueda parcial).
     *
     * @param nombre el nombre o apellido a buscar
     * @return una lista de usuarios que coinciden con la búsqueda
     */
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.nombres) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
            "OR LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Usuario> buscarPorNombre(@Param("nombre") String nombre);

    /**
     * Verifica si existe un usuario con el correo especificado.
     *
     * @param correo el correo electrónico a verificar
     * @return true si existe un usuario con ese correo, false en caso contrario
     */
    boolean existsByCorreo(String correo);

    /**
     * Verifica si existe un usuario con la identificación especificada.
     *
     * @param identificacion la identificación a verificar
     * @return true si existe un usuario con esa identificación, false en caso contrario
     */
    boolean existsByIdentificacion(String identificacion);

    List<Usuario> findByRolAndFechaCreacionBetween(RolUsuario rol, LocalDateTime inicio, LocalDateTime fin);
}