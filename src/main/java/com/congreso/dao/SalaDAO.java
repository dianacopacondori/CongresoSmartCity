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

@Repository
public interface SalaDAO extends JpaRepository<Sala, Long>, DAO<Sala, Long> {

    List<Sala> findByCapacidadGreaterThanEqual(Integer capacidad);

    @Query("SELECT s FROM Sala s WHERE s.codigo NOT IN " +
           "(SELECT a.sala.codigo FROM Actividad a WHERE a.fecha = :fecha " +
           "AND a.horarioInicio < :horaFin AND a.horarioFin > :horaInicio " +
           "AND a.estado = com.congreso.modelo.enums.EstadoActividad.ACTIVA)")
    List<Sala> buscarSalasDisponibles(@Param("fecha") LocalDate fecha,
                                      @Param("horaInicio") LocalTime horaInicio,
                                      @Param("horaFin") LocalTime horaFin);
}
