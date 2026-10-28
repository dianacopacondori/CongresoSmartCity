package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Fotocheck;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoFotocheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Fotocheck.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Repository
public interface FotocheckDAO extends JpaRepository<Fotocheck, Long> {

    /**
     * Busca un fotocheck por su código único (usado en el escáner de recepción).
     */
    Optional<Fotocheck> findByCodigo(Long codigo);

    /**
     * Busca el fotocheck asociado a un usuario específico.
     */
    Optional<Fotocheck> findByUsuario(Usuario usuario);

    /**
     * Obtiene todos los fotochecks con un estado específico (ej. EMITIDO o ANULADO).
     */
    List<Fotocheck> findByEstado(EstadoFotocheck estado);

    /**
     * Verifica si un usuario ya tiene un fotocheck generado.
     */
    boolean existsByUsuario(Usuario usuario);
}
