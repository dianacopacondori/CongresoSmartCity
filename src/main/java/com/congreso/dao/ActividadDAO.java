package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Sala;
import com.congreso.modelo.enums.EstadoActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para la entidad Actividad (Padre de Ponencia y Taller).
 * Incluye consultas específicas para ponencias, talleres y validación de salas.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Repository
public interface ActividadDAO extends JpaRepository<Actividad, Long>  {

    /**
     * Obtiene solo las actividades que están activas y tienen cupo disponible.
     * Regla de negocio: "El sistema publica únicamente actividades activas y con capacidad disponible".
     */
    List<Actividad> findByEstadoAndInscritosLessThan(EstadoActividad estado, Integer cupo);

    /**
     * Obtiene actividades de un día específico para la agenda.
     */
    List<Actividad> findByFechaAndEstadoOrderByHorarioInicio(LocalDate fecha, EstadoActividad estado);

    /**
     * Lista todas las ponencias disponibles (activas y con cupo).
     * Usa TYPE() para filtrar por tipo de entidad en herencia JOINED.
     */
    @Query("SELECT p FROM Ponencia p WHERE p.estado = :estado AND p.inscritos < p.cupo")
    List<com.congreso.modelo.Ponencia> listarPonenciasDisponibles(@Param("estado") EstadoActividad estado);

    /**
     * Lista todas las ponencias disponibles (sobrecarga sin parámetros).
     */
    @Query("SELECT p FROM Ponencia p WHERE p.estado = 'ACTIVA' AND p.inscritos < p.cupo")
    List<com.congreso.modelo.Ponencia> listarPonenciasDisponibles();

    /**
     * Lista todos los talleres disponibles (activos y con cupo).
     */
    @Query("SELECT t FROM Taller t WHERE t.estado = :estado AND t.inscritos < t.cupo")
    List<com.congreso.modelo.Taller> listarTalleresDisponibles(@Param("estado") EstadoActividad estado);

    /**
     * Lista todos los talleres disponibles (sobrecarga sin parámetros).
     */
    @Query("SELECT t FROM Taller t WHERE t.estado = 'ACTIVA' AND t.inscritos < t.cupo")
    List<com.congreso.modelo.Taller> listarTalleresDisponibles();

    /**
     * Busca actividades asignadas a una sala en una fecha específica.
     * Útil para validar disponibilidad de salas y evitar cruces.
     */
    List<Actividad> findBySalaAndFecha(Sala sala, LocalDate fecha);

    /**
     * Verifica si existe una actividad en una sala en un horario específico.
     * Regla de negocio: No permitir dos actividades en la misma sala al mismo tiempo.
     *
     * @return true si existe conflicto (la sala está ocupada), false si está libre.
     */
    @Query("SELECT COUNT(a) > 0 FROM Actividad a " +
            "WHERE a.sala = :sala AND a.fecha = :fecha AND a.estado = 'ACTIVA' " +
            "AND ((a.horarioInicio < :horarioFin) AND (a.horarioFin > :horarioInicio))")
    boolean existeConflictoDeHorarioEnSala(@Param("sala") Sala sala,
                                           @Param("fecha") LocalDate fecha,
                                           @Param("horarioInicio") java.time.LocalTime horarioInicio,
                                           @Param("horarioFin") java.time.LocalTime horarioFin);

    /**
     * Obtiene todas las actividades activas (para el portal público y admin).
     */
    List<Actividad> findByEstado(EstadoActividad estado);

    /**
     * Busca actividades por título (búsqueda parcial, útil para filtros).
     */
    @Query("SELECT a FROM Actividad a WHERE LOWER(a.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))")
    List<Actividad> buscarPorTitulo(@Param("titulo") String titulo);

    /**
     * Cuenta cuántas actividades activas existen en total.
     * Útil para estadísticas del dashboard admin.
     */
    long countByEstado(EstadoActividad estado);
}