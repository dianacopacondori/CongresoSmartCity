package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Notificacion;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacionDAO extends JpaRepository<Notificacion, Long>, DAO<Notificacion, Long> {

    List<Notificacion> findByDestinatario(Usuario destinatario);

    List<Notificacion> findByEstadoEnvio(EstadoNotificacion estado);

    @Query("SELECT n FROM Notificacion n WHERE n.estadoEnvio = com.congreso.modelo.enums.EstadoNotificacion.PENDIENTE")
    List<Notificacion> listarPendientes();

    @Query("SELECT n FROM Notificacion n WHERE n.destinatario = :usuario AND n.fecha BETWEEN :inicio AND :fin")
    List<Notificacion> buscarPorUsuarioYFechas(@Param("usuario") Usuario usuario,
                                               @Param("inicio") LocalDateTime inicio,
                                               @Param("fin") LocalDateTime fin);
}
