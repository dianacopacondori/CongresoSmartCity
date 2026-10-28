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

        // Validación de regla de negocio: No permitir doble registro
        if (asistenciaDAO.findByAsistenteAndActividad(asistente, actividad).isPresent()) {
            throw new IllegalStateException("La asistencia para esta actividad ya fue registrada");
        }

        Asistencia asistencia = new Asistencia();
        asistencia.setAsistente(asistente);
        asistencia.setActividad(actividad);
        asistencia.setFecha(LocalDate.now());
        asistencia.setHora(LocalTime.now());
        asistencia.setMecanismoValidacion(mecanismoValidacion);
        asistencia.setEstado(EstadoAsistencia.PRESENTE);

        return asistenciaDAO.guardarConRegistro(asistencia);
    }

    public Asistencia corregirAsistencia(Long codigoAsistencia, EstadoAsistencia estado) {
        if (estado != EstadoAsistencia.PRESENTE && estado != EstadoAsistencia.AUSENTE) {
            throw new IllegalArgumentException("El estado corregido debe ser PRESENTE o AUSENTE");
        }

        Asistencia asistencia = asistenciaDAO.findById(codigoAsistencia)
                .orElseThrow(() -> new IllegalArgumentException("Asistencia no encontrada"));
        asistencia.setEstado(estado);
        return asistenciaDAO.guardarConRegistro(asistencia);
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

    // --- MÉTODOS AGREGADOS PARA EL SERVLET ---

    /**
     * Alias para que el servlet pueda llamarlo con este nombre.
     */
    public List<Asistencia> consultarAsistenciasPorActividad(Long codigoActividad) {
        return listarAsistenciasPorActividad(codigoActividad);
    }

    /**
     * Obtiene todas las asistencias del sistema (para el administrador).
     */
    public List<Asistencia> obtenerTodasLasAsistencias() {
        return asistenciaDAO.findAll();
    }

    /**
     * Corrige el estado de una asistencia a "CORREGIDA" (solo administrador).
     */
    public void corregirAsistencia(Long codigoAsistencia, String justificacion) {
        Asistencia asistencia = asistenciaDAO.findById(codigoAsistencia)
                .orElseThrow(() -> new IllegalArgumentException("Registro de asistencia no encontrado"));

        asistencia.setEstado(EstadoAsistencia.CORREGIDA);
        // Nota: Si agregas un campo 'observaciones' a la entidad Asistencia, puedes guardarlo aquí:
        // asistencia.setObservaciones("Corregido por admin. Justificación: " + justificacion);

        asistenciaDAO.save(asistencia);
    }

    public long contarPresentes(Long codigoActividad) {
        Actividad actividad = actividadDAO.findById(codigoActividad)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        return asistenciaDAO.countByActividadAndEstado(actividad, EstadoAsistencia.PRESENTE);
    }
}