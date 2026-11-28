package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Asistencia;
import com.congreso.modelo.enums.EstadoAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaDAO extends JpaRepository<Asistencia, Long>{

    // Regla de negocio: Previene el doble registro de asistencia
    boolean existsByAsistenteAndActividadAndFechaAndEstadoIn(
            Asistente asistente,
            Actividad actividad,
            LocalDate fecha,
            List<EstadoAsistencia> estados);

    // Método solicitado por AsistenciaServicio
    Optional<Asistencia> findByAsistenteAndActividad(Asistente asistente, Actividad actividad);

    // Método solicitado por AsistenciaServicio
    List<Asistencia> findByActividad(Actividad actividad);

    // Alias para mantener compatibilidad con tu servicio
    default List<Asistencia> findByAsistente(Asistente asistente) {
        return findByAsistenteOrderByFechaDesc(asistente);
    }

    List<Asistencia> findByAsistenteOrderByFechaDesc(Asistente asistente);

    List<Asistencia> findByFechaBetween(LocalDate fechaInicio, LocalDate fechaFin);

    // Método solicitado por AsistenciaServicio para contar presentes
    long countByActividadAndEstado(Actividad actividad, EstadoAsistencia estado);
}