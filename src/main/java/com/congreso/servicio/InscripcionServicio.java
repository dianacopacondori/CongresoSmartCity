package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.RolUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InscripcionServicio {

    private final InscripcionDAO inscripcionDAO;
    private final AsistenteDAO asistenteDAO;
    private final ActividadDAO actividadDAO;

    @Autowired
    public InscripcionServicio(InscripcionDAO inscripcionDAO,
                              AsistenteDAO asistenteDAO,
                              ActividadDAO actividadDAO) {
        this.inscripcionDAO = inscripcionDAO;
        this.asistenteDAO = asistenteDAO;
        this.actividadDAO = actividadDAO;
    }

    public boolean inscribirActividad(String correoAutenticado, Long codigoInscripcion, Long codigoActividad) {
        Asistente asistente = asistenteDAO.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new AccessDeniedException("Asistente no disponible"));
        if (!asistente.estaActivo() || asistente.getRol() != RolUsuario.ASISTENTE) {
            throw new AccessDeniedException("Asistente no disponible");
        }
        Inscripcion inscripcion = inscripcionDAO.findById(codigoInscripcion)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
        if (!inscripcion.getAsistente().getCodigo().equals(asistente.getCodigo())) {
            throw new AccessDeniedException("Inscripción ajena al asistente autenticado");
        }
        if (inscripcion.getEstado() != EstadoInscripcion.CONFIRMADA || !inscripcion.isConfirmacion()) {
            throw new IllegalStateException("La inscripción debe estar confirmada antes de elegir actividades");
        }
        Actividad actividad = actividadDAO.findById(codigoActividad)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        if (actividad.getEstado() != EstadoActividad.ACTIVA) {
            throw new IllegalStateException("La actividad no está activa");
        }

        if (!actividad.hayCupoDisponible()) {
            throw new IllegalStateException("No hay cupo disponible para esta actividad");
        }

        if (inscripcion.validarCrucesHorario(actividad)) {
            throw new IllegalStateException("La actividad se cruza con otra ya inscrita");
        }

        if (!inscripcion.agregarActividad(actividad)) {
            throw new IllegalStateException("No fue posible agregar esta actividad a la inscripción");
        }
        inscripcionDAO.saveAndFlush(inscripcion);
        return true;
    }

    public boolean confirmarInscripcion(Long codigoInscripcion) {
        Inscripcion inscripcion = inscripcionDAO.findById(codigoInscripcion)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
        inscripcion.confirmar();
        inscripcionDAO.save(inscripcion);
        return true;
    }

    public Inscripcion crearInscripcion(Asistente asistente, String tipo) {
        Inscripcion inscripcion = new Inscripcion(asistente, tipo);
        return inscripcionDAO.save(inscripcion);
    }

    public List<Inscripcion> listarInscripcionesPorAsistente(Asistente asistente) {
        return inscripcionDAO.findByAsistente(asistente);
    }

    public List<Inscripcion> listarInscripcionesConfirmadas(Asistente asistente) {
        return inscripcionDAO.buscarConfirmadasPorAsistente(asistente);
    }

    public List<Actividad> obtenerAgendaPersonalizada(Long codigoAsistente) {
        Asistente asistente = asistenteDAO.findById(codigoAsistente)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        return inscripcionDAO.buscarConfirmadasPorAsistente(asistente).stream()
                .flatMap(i -> i.getActividades().stream())
                .distinct()
                .sorted((a1, a2) -> {
                    int cmp = a1.getFecha().compareTo(a2.getFecha());
                    if (cmp != 0) return cmp;
                    return a1.getHorarioInicio().compareTo(a2.getHorarioInicio());
                })
                .toList();
    }

    public void eliminarInscripcion(Long codigo) {
        inscripcionDAO.deleteById(codigo);
    }

    /**
     * Lista todas las inscripciones del sistema (para el administrador).
     */
    public List<Inscripcion> listarTodasLasInscripciones() {
        return inscripcionDAO.findAll();
    }
}
