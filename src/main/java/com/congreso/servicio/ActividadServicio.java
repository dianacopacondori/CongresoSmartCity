package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Sala;
import com.congreso.modelo.Taller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
}
