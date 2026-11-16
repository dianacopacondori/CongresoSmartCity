package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.ColaboradorDAO;
import com.congreso.dao.SalaDAO;
import com.congreso.dto.ActividadFormulario;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Sala;
import com.congreso.modelo.Taller;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.modelo.enums.RolUsuario;
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
    private final SalaDAO salaDAO;
    private final ColaboradorDAO colaboradorDAO;

    @Autowired
    public ActividadServicio(ActividadDAO actividadDAO, SalaDAO salaDAO, ColaboradorDAO colaboradorDAO) {
        this.actividadDAO = actividadDAO;
        this.salaDAO = salaDAO;
        this.colaboradorDAO = colaboradorDAO;
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

    public List<Actividad> listarTodasLasActividades() {
        return actividadDAO.findAll();
    }

    public Actividad crearDesdeFormulario(ActividadFormulario form) {
        Actividad actividad = switch (form.getTipo()) {
            case "PONENCIA" -> new Ponencia();
            case "TALLER" -> new Taller();
            default -> throw new IllegalArgumentException("Tipo de actividad no válido");
        };
        completarActividad(actividad, form);
        return actividadDAO.save(actividad);
    }

    public Actividad actualizarDesdeFormulario(Long codigo, ActividadFormulario form) {
        Actividad actividad = actividadDAO.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        if (!form.getTipo().equals(actividad instanceof Ponencia ? "PONENCIA" : "TALLER")) {
            throw new IllegalArgumentException("No se puede cambiar el tipo de una actividad existente");
        }
        completarActividad(actividad, form);
        return actividadDAO.save(actividad);
    }

    private void completarActividad(Actividad actividad, ActividadFormulario form) {
        if (form.getHorarioInicio() == null || form.getHorarioFin() == null
                || !form.getHorarioInicio().isBefore(form.getHorarioFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }
        Sala sala = salaDAO.findById(form.getSalaId())
                .orElseThrow(() -> new IllegalArgumentException("Sala no encontrada"));
        if (form.getCupo() > sala.getCapacidad() || form.getCupo() < actividad.getInscritos()) {
            throw new IllegalArgumentException("El cupo debe ajustarse a la capacidad y a las inscripciones existentes");
        }
        boolean conflicto = actividadDAO.findBySalaAndFecha(sala, form.getFecha()).stream()
                .anyMatch(otra -> otra.getEstado() == EstadoActividad.ACTIVA
                        && !otra.getCodigo().equals(actividad.getCodigo())
                        && otra.getHorarioInicio().isBefore(form.getHorarioFin())
                        && otra.getHorarioFin().isAfter(form.getHorarioInicio()));
        if (conflicto) throw new IllegalArgumentException("La sala ya está ocupada en ese horario");

        Colaborador colaborador = colaboradorDAO.findById(form.getColaboradorId())
                .orElseThrow(() -> new IllegalArgumentException("Responsable no encontrado"));
        if (actividad instanceof Ponencia ponencia) {
            if (colaborador.getTipoColaborador() != RolUsuario.PONENTE) {
                throw new IllegalArgumentException("Selecciona un ponente válido");
            }
            ponencia.setPonente(colaborador);
            ponencia.setTema(form.getTema());
        } else if (actividad instanceof Taller taller) {
            if (colaborador.getTipoColaborador() != RolUsuario.INSTRUCTOR) {
                throw new IllegalArgumentException("Selecciona un instructor válido");
            }
            taller.setInstructor(colaborador);
            taller.setNivelDificultad(form.getNivelDificultad());
            taller.setMaterialesRequeridos(form.getMaterialesRequeridos());
            taller.setObjetivos(form.getObjetivos());
        }
        actividad.setTitulo(form.getTitulo());
        actividad.setDescripcion(form.getDescripcion());
        actividad.setFecha(form.getFecha());
        actividad.setHorarioInicio(form.getHorarioInicio());
        actividad.setHorarioFin(form.getHorarioFin());
        actividad.setCupo(form.getCupo());
        actividad.setSala(sala);
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
        return actividadDAO.findByEstado(EstadoActividad.ACTIVA).stream()
                .filter(Actividad::hayCupoDisponible).toList();
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
