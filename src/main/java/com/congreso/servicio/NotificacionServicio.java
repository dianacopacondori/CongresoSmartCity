package com.congreso.servicio;

import com.congreso.dao.NotificacionDAO;
import com.congreso.interfaces.Notificador;
import com.congreso.modelo.Notificacion;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoNotificacion;
import com.congreso.modelo.enums.TipoNotificacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de notificaciones.
 * Aplica el principio de responsabilidad única (SRP) de SOLID.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Service
@Transactional
public class NotificacionServicio {

    private final NotificacionDAO notificacionDAO;
    private final Notificador notificadorEmail;

    @Autowired
    public NotificacionServicio(NotificacionDAO notificacionDAO,
                                Notificador notificadorEmail) {
        this.notificacionDAO = notificacionDAO;
        this.notificadorEmail = notificadorEmail;
    }

    /**
     * Crea y envía una notificación.
     *
     * @param notificacion la notificación a enviar
     * @return la notificación con el estado actualizado
     */
    public Notificacion enviarNotificacion(Notificacion notificacion) {
        notificacion.setFecha(LocalDateTime.now());
        notificacion.setEstadoEnvio(EstadoNotificacion.PENDIENTE);

        // Guardar en base de datos
        notificacion = notificacionDAO.save(notificacion);

        // Intentar enviar por email
        boolean enviado = notificadorEmail.enviar(notificacion);

        if (enviado) {
            notificacion.setEstadoEnvio(EstadoNotificacion.ENVIADO);
        } else {
            notificacion.setEstadoEnvio(EstadoNotificacion.FALLIDO);
        }

        return notificacionDAO.save(notificacion);
    }

    /**
     * Envía una confirmación de inscripción.
     *
     * @param destinatario el usuario destinatario
     * @param asunto el asunto del correo
     * @param contenido el contenido del mensaje
     * @return la notificación creada
     */
    public Notificacion enviarConfirmacionInscripcion(Usuario destinatario,
                                                      String asunto,
                                                      String contenido) {
        Notificacion notificacion = new Notificacion(
                destinatario,
                TipoNotificacion.CONFIRMACION_INSCRIPCION,
                asunto,
                contenido
        );

        return enviarNotificacion(notificacion);
    }

    /**
     * Envía la agenda personalizada al asistente.
     *
     * @param destinatario el usuario destinatario
     * @param horarioPersonalizado la agenda en formato texto o HTML
     * @return la notificación creada
     */
    public Notificacion enviarAgendaPersonalizada(Usuario destinatario,
                                                  String horarioPersonalizado) {
        Notificacion notificacion = new Notificacion(
                destinatario,
                TipoNotificacion.AGENDA_PERSONALIZADA,
                "Tu agenda personalizada - Congreso Smart City",
                "Estimado participante,\n\nAdjuntamos tu agenda personalizada:\n\n" +
                        horarioPersonalizado
        );

        notificacion.setHorarioPersonalizado(horarioPersonalizado);

        return enviarNotificacion(notificacion);
    }

    /**
     * Envía un recordatorio de actividad.
     *
     * @param destinatario el usuario destinatario
     * @param asunto el asunto del correo
     * @param contenido el contenido del mensaje
     * @return la notificación creada
     */
    public Notificacion enviarRecordatorio(Usuario destinatario,
                                           String asunto,
                                           String contenido) {
        Notificacion notificacion = new Notificacion(
                destinatario,
                TipoNotificacion.RECORDATORIO,
                asunto,
                contenido
        );

        return enviarNotificacion(notificacion);
    }

    /**
     * Busca una notificación por su código.
     *
     * @param codigo el código de la notificación
     * @return un Optional con la notificación encontrada, o vacío si no existe
     */
    public Optional<Notificacion> buscarPorCodigo(Long codigo) {
        return notificacionDAO.findById(codigo);
    }

    /**
     * Obtiene las notificaciones de un usuario.
     *
     * @param usuario el usuario
     * @return una lista de notificaciones del usuario
     */
    public List<Notificacion> obtenerNotificacionesPorUsuario(Usuario usuario) {
        return notificacionDAO.findByDestinatarioOrderByFechaDesc(usuario);
    }

    /**
     * Obtiene notificaciones fallidas para reintentar el envío.
     *
     * @return una lista de notificaciones con estado fallido
     */
    public List<Notificacion> obtenerNotificacionesFallidas() {
        return notificacionDAO.findByEstadoEnvio(EstadoNotificacion.FALLIDO);
    }

    /**
     * Reenvía una notificación fallida.
     *
     * @param notificacionId el código de la notificación a reenviar
     * @return la notificación con el estado actualizado
     * @throws IllegalArgumentException si la notificación no existe
     */
    public Notificacion reenviarNotificacion(Long notificacionId) {
        Notificacion notificacion = notificacionDAO.findById(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        notificacion.setEstadoEnvio(EstadoNotificacion.PENDIENTE);
        return enviarNotificacion(notificacion);
    }

    /**
     * Marca una notificación como leída (cambia estado a ENVIADO si estaba PENDIENTE).
     *
     * @param notificacionId el código de la notificación
     */
    public void marcarComoLeida(Long notificacionId) {
        Notificacion notificacion = notificacionDAO.findById(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        if (notificacion.getEstadoEnvio() == EstadoNotificacion.PENDIENTE) {
            notificacion.setEstadoEnvio(EstadoNotificacion.ENVIADO);
            notificacionDAO.save(notificacion);
        }
    }
}
