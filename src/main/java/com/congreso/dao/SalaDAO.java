package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repositorio para la entidad Sala.
 * Incluye validaciones de disponibilidad para evitar cruces de horario.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Repository
public interface SalaDAO extends JpaRepository<Sala, Long> {

    /**
     * Busca salas que tengan una capacidad mínima requerida.
     */
    List<Sala> findByCapacidadGreaterThanEqual(Integer capacidadMinima);

    /**
     * Verifica si una sala esta ocupada en un rango de horario especifico.
     * Regla de negocio: No permitir asignar dos actividades a la misma sala en el mismo horario.
     *
     * @return true si existe conflicto (la sala NO está disponible), false si está libre.
     */
    @Query("SELECT COUNT(s) > 0 FROM Actividad a JOIN a.sala s " +
            "WHERE s.codigo = :salaId AND a.fecha = :fecha " +
            "AND ((a.horarioInicio < :horarioFin) AND (a.horarioFin > :horarioInicio)) " +
            "AND a.estado = 'ACTIVA'")
    boolean existeConflictoDeHorario(@Param("salaId") Long salaId,
                                     @Param("fecha") LocalDate fecha,
                                     @Param("horarioInicio") LocalTime horarioInicio,
                                     @Param("horarioFin") LocalTime horarioFin);

    /**
     * Método auxiliar para el servicio: devuelve true si la sala SÍ está disponible.
     */
    default boolean estaDisponible(Long salaId, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        return !existeConflictoDeHorario(salaId, fecha, inicio, fin);
    }
}