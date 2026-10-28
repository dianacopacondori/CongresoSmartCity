package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Sala;
import com.congreso.modelo.Taller;
import com.congreso.modelo.enums.EstadoActividad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ActividadServicio {

    private final ActividadDAO actividadDAO;

    @Autowired
    public ActividadServicio(ActividadDAO actividadDAO) {
        this.actividadDAO = actividadDAO;
    }

    public List<Ponencia> listarPonenciasDisponibles() {
        return actividadDAO.listarPonenciasDisponibles();
    }

    public List<Taller> listarTalleresDisponibles() {
        return actividadDAO.listarTalleresDisponibles();
    }

    public List<Actividad> listarActividadesPorSalaYFecha(Sala sala, LocalDate fecha) {
        return actividadDAO.findBySalaAndFecha(sala, fecha);
    }

    public Actividad crearActividad(Actividad actividad) {
        return actividadDAO.save(actividad);
    }

    public Actividad actualizarActividad(Actividad actividad) {
        return actividadDAO.save(actividad);
    }

    public void eliminarActividad(Long codigo) {
        actividadDAO.deleteById(codigo);
    }

    /**
     * Obtiene todas las actividades activas con cupo disponible.
     * Usado por el servlet para mostrar actividades disponibles al asistente.
     */
    public List<Actividad> obtenerActividadesActivas() {
        return actividadDAO.findByEstadoAndInscritosLessThan(EstadoActividad.ACTIVA, 999999);
    }

    /**
     * Busca una actividad por su código.
     * Usado por el servlet para editar actividades.
     */
    public Optional<Actividad> buscarPorId(Long codigo) {
        return actividadDAO.findById(codigo);
    }

    /**
     * Cancela una actividad cambiando su estado a CANCELADA.
     * Usado por el servlet para eliminar lógicamente actividades.
     */
    public void cancelarActividad(Long codigo) {
        Actividad actividad = actividadDAO.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        actividad.setEstado(EstadoActividad.CANCELADA);
        actividadDAO.save(actividad);
    }
}