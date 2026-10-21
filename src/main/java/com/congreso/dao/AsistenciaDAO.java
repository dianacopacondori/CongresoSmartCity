package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Asistencia;
import com.congreso.modelo.enums.EstadoAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaDAO extends JpaRepository<Asistencia, Long>, DAO<Asistencia, Long>,
        AsistenciaDAOOperaciones {

    List<Asistencia> findByAsistente(Asistente asistente);

    List<Asistencia> findByActividad(Actividad actividad);

    List<Asistencia> findByFecha(LocalDate fecha);

    @Query("SELECT a FROM Asistencia a WHERE a.asistente = :asistente AND a.actividad = :actividad")
    Optional<Asistencia> buscarPorAsistenteYActividad(@Param("asistente") Asistente asistente,
                                                      @Param("actividad") Actividad actividad);

    @Query("SELECT a FROM Asistencia a WHERE a.fecha BETWEEN :inicio AND :fin")
    List<Asistencia> buscarPorRangoFechas(@Param("inicio") LocalDate inicio,
                                          @Param("fin") LocalDate fin);

    long countByActividadAndEstado(Actividad actividad, EstadoAsistencia estado);
}
