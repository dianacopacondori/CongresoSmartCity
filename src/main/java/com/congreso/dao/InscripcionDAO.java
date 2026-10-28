package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.enums.EstadoInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InscripcionDAO extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByAsistente(Asistente asistente);

    List<Inscripcion> findByEstado(EstadoInscripcion estado);

    @Query("SELECT i FROM Inscripcion i WHERE i.asistente = :asistente AND i.estado = com.congreso.modelo.enums.EstadoInscripcion.CONFIRMADA")
    List<Inscripcion> buscarConfirmadasPorAsistente(@Param("asistente") Asistente asistente);

    @Query("SELECT i FROM Inscripcion i JOIN i.actividades a WHERE a = :actividad")
    List<Inscripcion> buscarPorActividad(@Param("actividad") Actividad actividad);
}
