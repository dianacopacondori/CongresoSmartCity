package com.congreso.servicio;

import com.congreso.dao.ActividadDAO;
import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.seguridad.RegistroAuditoria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class InscripcionServicio {

    private final InscripcionDAO inscripcionDAO;
    private final AsistenteDAO asistenteDAO;
    private final ActividadDAO actividadDAO;
    private final PagoDAO pagoDAO;

    @Autowired
    public InscripcionServicio(InscripcionDAO inscripcionDAO,
                               AsistenteDAO asistenteDAO,
                               ActividadDAO actividadDAO, PagoDAO pagoDAO) {
        this.inscripcionDAO = inscripcionDAO;
        this.asistenteDAO = asistenteDAO;
        this.actividadDAO = actividadDAO;
        this.pagoDAO = pagoDAO;
    }

    public boolean inscribirActividad(String correoAutenticado, Long codigoInscripcion, Long codigoActividad) {
        Asistente asistente = asistenteAutenticado(correoAutenticado);
        Inscripcion inscripcion = inscripcionDAO.bloquearPorCodigo(codigoInscripcion)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
        if (!inscripcion.getAsistente().getCodigo().equals(asistente.getCodigo())) {
            throw new AccessDeniedException("Inscripción ajena al asistente autenticado");
        }
        return agregarActividadConfirmada(inscripcion, codigoActividad);
    }

    public boolean inscribirActividadPropia(String correoAutenticado, Long codigoActividad) {
        Inscripcion elegida = inscripcionDisponible(correoAutenticado)
                .orElseThrow(() -> new IllegalStateException("No tienes una inscripción confirmada con pago completo"));
        return inscribirActividad(correoAutenticado, elegida.getCodigo(), codigoActividad);
    }

    @Transactional(readOnly = true)
    public Optional<Inscripcion> inscripcionDisponible(String correoAutenticado) {
        Asistente asistente = asistenteAutenticado(correoAutenticado);
        return inscripcionDAO.findByAsistente(asistente).stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.CONFIRMADA && i.isConfirmacion())
                .filter(this::pagoCompleto)
                .max(Comparator.comparing(Inscripcion::getCodigo));
    }

    private Asistente asistenteAutenticado(String correoAutenticado) {
        Asistente asistente = asistenteDAO.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new AccessDeniedException("Asistente no disponible"));
        if (!asistente.estaActivo() || asistente.getRol() != RolUsuario.ASISTENTE) {
            throw new AccessDeniedException("Asistente no disponible");
        }
        return asistente;
    }

    private boolean pagoCompleto(Inscripcion inscripcion) {
        return pagoDAO.findByInscripcion(inscripcion)
                .filter(p -> p.getEstado() == EstadoPago.PAGADO && p.getSaldo() == 0
                        && p.getAsistente().getCodigo().equals(inscripcion.getAsistente().getCodigo()))
                .isPresent();
    }

    private boolean agregarActividadConfirmada(Inscripcion inscripcion, Long codigoActividad) {
        if (inscripcion.getEstado() != EstadoInscripcion.CONFIRMADA || !inscripcion.isConfirmacion()) {
            throw new IllegalStateException("La inscripción debe estar confirmada antes de elegir actividades");
        }
        if (!pagoCompleto(inscripcion)) {
            throw new IllegalStateException("Debes completar el pago simulado antes de elegir actividades");
        }
        if (inscripcion.getActividades().stream().anyMatch(a -> a.getCodigo().equals(codigoActividad))) {
            throw new IllegalStateException("Ya tienes esta actividad en tu inscripción");
        }
        actividadDAO.bloquearCodigo(codigoActividad)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
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

    /**
     * Cancela una inscripción de forma transaccional: bloquea la fila, libera un cupo
     * por actividad distinta, limpia la tabla intermedia y conserva el historial.
     * No se cancelan inscripciones con importes ya cubiertos: la política de
     * devoluciones sigue pendiente (D03).
     */
    public Inscripcion cancelarInscripcion(Long codigo, String actor) {
        Inscripcion inscripcion = inscripcionDAO.bloquearPorCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            throw new IllegalStateException("La inscripción ya está cancelada");
        }

        pagoDAO.findByInscripcion(inscripcion).ifPresent(pago -> {
            boolean conImporteCubierto = pago.getEstado() == EstadoPago.PAGADO
                    || pago.getEstado() == EstadoPago.PARCIAL
                    || pago.getImporte() - pago.getSaldo() > 0;
            if (conImporteCubierto) {
                throw new IllegalStateException(
                        "No se puede cancelar una inscripción con pagos cubiertos; requiere política de devolución");
            }
        });

        Map<Long, Actividad> actividadesUnicas = new LinkedHashMap<>();
        for (Actividad actividad : inscripcion.getActividades()) {
            actividadesUnicas.putIfAbsent(actividad.getCodigo(), actividad);
        }

        for (Long codigoActividad : actividadesUnicas.keySet()) {
            actividadDAO.bloquearCodigo(codigoActividad);
            Actividad actividad = actividadDAO.findById(codigoActividad).orElse(null);
            if (actividad == null) continue;
            int inscritos = actividad.getInscritos() == null ? 0 : actividad.getInscritos();
            actividad.setInscritos(Math.max(0, inscritos - 1));
            actividadDAO.save(actividad);
        }

        inscripcion.getActividades().clear();
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        inscripcionDAO.saveAndFlush(inscripcion);

        RegistroAuditoria.registrar("CANCELAR", "INSCRIPCION", codigo, actor,
                "cupos-liberados=" + actividadesUnicas.size() + "; sin importes cubiertos");
        return inscripcion;
    }

    /**
     * Lista todas las inscripciones del sistema (para el administrador).
     */
    public List<Inscripcion> listarTodasLasInscripciones() {
        return inscripcionDAO.findAll();
    }
}
