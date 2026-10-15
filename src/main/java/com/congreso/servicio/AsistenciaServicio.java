package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.AsistenciaDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Asistencia;
import com.congreso.modelo.enums.EstadoAsistencia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class AsistenciaServicio {

    private final AsistenciaDAO asistenciaDAO;
    private final AsistenteDAO asistenteDAO;
    private final ActividadDAO actividadDAO;

    @Autowired
    public AsistenciaServicio(AsistenciaDAO asistenciaDAO,
                              AsistenteDAO asistenteDAO,
                              ActividadDAO actividadDAO) {
        this.asistenciaDAO = asistenciaDAO;
        this.asistenteDAO = asistenteDAO;
        this.actividadDAO = actividadDAO;
    }

    public Asistencia registrarAsistencia(Long codigoAsistente, Long codigoActividad, String mecanismoValidacion) {
        Asistente asistente = asistenteDAO.findById(codigoAsistente)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        Actividad actividad = actividadDAO.findById(codigoActividad)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        if (asistenciaDAO.buscarPorAsistenteYActividad(asistente, actividad).isPresent()) {
            throw new IllegalStateException("La asistencia ya fue registrada");
        }

        Asistencia asistencia = new Asistencia();
        asistencia.setAsistente(asistente);
        asistencia.setActividad(actividad);
        asistencia.setFecha(LocalDate.now());
        asistencia.setHora(LocalTime.now());
        asistencia.setMecanismoValidacion(mecanismoValidacion);
        asistencia.setEstado(EstadoAsistencia.PRESENTE);

        return asistenciaDAO.save(asistencia);
    }

    public List<Asistencia> listarAsistenciasPorAsistente(Long codigoAsistente) {
        Asistente asistente = asistenteDAO.findById(codigoAsistente)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        return asistenciaDAO.findByAsistente(asistente);
    }

    public List<Asistencia> listarAsistenciasPorActividad(Long codigoActividad) {
        Actividad actividad = actividadDAO.findById(codigoActividad)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        return asistenciaDAO.findByActividad(actividad);
    }

    public long contarPresentes(Long codigoActividad) {
        Actividad actividad = actividadDAO.findById(codigoActividad)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        return asistenciaDAO.countByActividadAndEstado(actividad, EstadoAsistencia.PRESENTE);
    }
}
