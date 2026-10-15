package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Taller;
import com.congreso.modelo.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ActividadDAO extends JpaRepository<Actividad, Long>, DAO<Actividad, Long> {

    @Query("SELECT p FROM Ponencia p WHERE p.estado = com.congreso.modelo.enums.EstadoActividad.ACTIVA AND p.inscritos < p.cupo")
    List<Ponencia> listarPonenciasDisponibles();

    @Query("SELECT t FROM Taller t WHERE t.estado = com.congreso.modelo.enums.EstadoActividad.ACTIVA AND t.inscritos < t.cupo")
    List<Taller> listarTalleresDisponibles();

    List<Ponencia> findByFechaPonencia(LocalDate fecha);

    List<Taller> findByFechaTaller(LocalDate fecha);

    @Query("SELECT a FROM Actividad a WHERE a.sala = :sala AND a.fecha = :fecha AND a.estado = com.congreso.modelo.enums.EstadoActividad.ACTIVA")
    List<Actividad> findBySalaAndFecha(@Param("sala") Sala sala, @Param("fecha") LocalDate fecha);
}
