package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.enums.EstadoInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface InscripcionDAO extends JpaRepository<Inscripcion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inscripcion i WHERE i.codigo = :codigo")
    Optional<Inscripcion> bloquearPorCodigo(@Param("codigo") Long codigo);

    List<Inscripcion> findByAsistente(Asistente asistente);

    List<Inscripcion> findByEstado(EstadoInscripcion estado);

    @Query("SELECT i FROM Inscripcion i WHERE i.asistente = :asistente AND i.estado = com.congreso.modelo.enums.EstadoInscripcion.CONFIRMADA")
    List<Inscripcion> buscarConfirmadasPorAsistente(@Param("asistente") Asistente asistente);

    @Query("SELECT i FROM Inscripcion i JOIN i.actividades a WHERE a = :actividad")
    List<Inscripcion> buscarPorActividad(@Param("actividad") Actividad actividad);
}
