package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Notificacion;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Notificacion.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Repository
public interface NotificacionDAO extends JpaRepository<Notificacion, Long> {

    /**
     * Obtiene el historial de notificaciones de un usuario, ordenadas de la más reciente a la más antigua.
     */
    List<Notificacion> findByDestinatarioOrderByFechaDesc(Usuario destinatario);

    /**
     * Busca notificaciones que fallaron en el envío para procesos de reintento (cron jobs).
     */
    List<Notificacion> findByEstadoEnvio(EstadoNotificacion estadoEnvio);

    /**
     * Cuenta cuántas notificaciones de un tipo específico se enviaron a un usuario.
     */
    long countByDestinatarioAndTipo(Usuario destinatario, com.congreso.modelo.enums.TipoNotificacion tipo);
}
