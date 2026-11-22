package com.congreso.servicio;

import com.congreso.dao.AsistenciaDAO;
import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.ActividadDAO;
import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Actividad;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Asistencia;
import com.congreso.modelo.Fotocheck;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.modelo.enums.EstadoAsistencia;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.seguridad.RegistroAuditoria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class AsistenciaServicio {

    private final AsistenciaDAO asistenciaDAO;
    private final AsistenteDAO asistenteDAO;
    private final ActividadDAO actividadDAO;
    private final InscripcionServicio inscripcionServicio;
    private final FotocheckServicio fotocheckServicio;
    private final UsuarioDAO usuarioDAO;

    @Autowired
    public AsistenciaServicio(AsistenciaDAO asistenciaDAO,
                              AsistenteDAO asistenteDAO,
                              ActividadDAO actividadDAO,
                              InscripcionServicio inscripcionServicio,
                              FotocheckServicio fotocheckServicio,
                              UsuarioDAO usuarioDAO) {
        this.asistenciaDAO = asistenciaDAO;
        this.asistenteDAO = asistenteDAO;
        this.actividadDAO = actividadDAO;
        this.inscripcionServicio = inscripcionServicio;
        this.fotocheckServicio = fotocheckServicio;
        this.usuarioDAO = usuarioDAO;
    }

    /** Firma anterior, mantenida para llamadas internas sin registrador ni credencial. */
    public Asistencia registrarAsistencia(Long codigoAsistente, Long codigoActividad, String mecanismoValidacion) {
        return registrarAsistencia(codigoAsistente, codigoActividad, mecanismoValidacion, null, null);
    }

    /**
     * Registra asistencia solo si el asistente está inscrito en esa actividad con
     * inscripción confirmada y pago completo, y si la credencial presentada es válida.
     */
    public Asistencia registrarAsistencia(Long codigoAsistente, Long codigoActividad,
                                          String mecanismoValidacion, Long codigoFotocheck,
                                          String registradorCorreo) {
        Asistente asistente = asistenteDAO.findById(codigoAsistente)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        Actividad actividad = actividadDAO.findById(codigoActividad)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        if (actividad.getEstado() != EstadoActividad.ACTIVA) {
            throw new IllegalStateException("La actividad no está activa");
        }
        if (actividad.getFecha() == null) {
            throw new IllegalStateException("La actividad no tiene fecha programada");
        }

        Usuario registrador = registrar(registradorCorreo);
        validarInscripcion(asistente, actividad);
        String mecanismo = validarCredencial(mecanismoValidacion, codigoFotocheck, asistente, registrador);

        if (asistenciaDAO.findByAsistenteAndActividad(asistente, actividad).isPresent()) {
            throw new IllegalStateException("La asistencia para esta actividad ya fue registrada");
        }

        Asistencia asistencia = new Asistencia();
        asistencia.setAsistente(asistente);
        asistencia.setActividad(actividad);
        asistencia.setFecha(actividad.getFecha());
        asistencia.setHora(LocalTime.now());
        asistencia.setMecanismoValidacion(mecanismo);
        asistencia.setRegistradoPor(registrador);
        asistencia.setEstado(EstadoAsistencia.PRESENTE);

        Asistencia guardada;
        try {
            guardada = asistenciaDAO.saveAndFlush(asistencia);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("La asistencia para esta actividad ya fue registrada");
        }
        RegistroAuditoria.registrar("REGISTRAR", "ASISTENCIA", guardada.getCodigo(),
                registradorCorreo, "mecanismo=" + mecanismo);
        return guardada;
    }

    private Usuario registrar(String correo) {
        return administrador(correo);
    }

    private Usuario administrador(String correo) {
        if (correo == null || correo.isBlank()) return null;
        Usuario usuario = usuarioDAO.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (usuario.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new AccessDeniedException("Solo el personal administrativo puede realizar esta acción");
        }
        return usuario;
    }

    private void validarInscripcion(Asistente asistente, Actividad actividad) {
        Inscripcion elegible = inscripcionServicio.inscripcionDisponible(asistente.getCorreo())
                .orElseThrow(() -> new IllegalStateException(
                        "El asistente no tiene una inscripción confirmada con el pago simulado completo"));
        boolean enAgenda = elegible.getActividades().stream()
                .anyMatch(a -> a.getCodigo().equals(actividad.getCodigo()));
        if (!enAgenda) {
            throw new IllegalStateException("El asistente no tiene esta actividad en su agenda confirmada");
        }
    }

    private String validarCredencial(String mecanismoValidacion, Long codigoFotocheck,
                                     Asistente asistente, Usuario registrador) {
        String mecanismo = mecanismoValidacion == null ? "" : mecanismoValidacion.trim().toUpperCase(Locale.ROOT);
        if (mecanismo.equals("MANUAL")) {
            if (registrador == null) {
                throw new IllegalStateException("El registro manual requiere un administrador identificable");
            }
            return mecanismo;
        }
        if (mecanismo.equals("QR") || mecanismo.equals("CODIGO")) {
            if (codigoFotocheck == null) {
                throw new IllegalArgumentException("Debe indicar el código del fotocheck");
            }
            Fotocheck fotocheck = fotocheckServicio.buscarPorCodigo(codigoFotocheck)
                    .orElseThrow(() -> new IllegalArgumentException("Fotocheck no encontrado"));
            boolean pertenece = fotocheck.getUsuario() != null
                    && asistente.getCodigo().equals(fotocheck.getUsuario().getCodigo());
            if (!pertenece || !fotocheckServicio.validarFotocheck(codigoFotocheck)) {
                throw new IllegalStateException("El fotocheck presentado no es válido para este asistente");
            }
            return mecanismo;
        }
        throw new IllegalArgumentException("Mecanismo de validación no permitido");
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

    public List<Asistencia> consultarAsistenciasPorActividad(Long codigoActividad) {
        return listarAsistenciasPorActividad(codigoActividad);
    }

    public List<Asistencia> obtenerTodasLasAsistencias() {
        return asistenciaDAO.findAll();
    }

    /** Firma anterior: sin administrador autenticado la corrección se rechaza. */
    public void corregirAsistencia(Long codigoAsistencia, String justificacion) {
        corregirAsistencia(codigoAsistencia, justificacion, null);
    }

    public void corregirAsistencia(Long codigoAsistencia, String justificacion, String correctorCorreo) {
        Usuario corrector = administrador(correctorCorreo);
        if (corrector == null) {
            throw new AccessDeniedException("La corrección requiere un administrador autenticado");
        }
        if (justificacion == null || justificacion.trim().length() < 5) {
            throw new IllegalArgumentException("La justificación de la corrección es obligatoria");
        }

        Asistencia asistencia = asistenciaDAO.findById(codigoAsistencia)
                .orElseThrow(() -> new IllegalArgumentException("Registro de asistencia no encontrado"));

        String motivo = justificacion.trim();
        asistencia.setEstado(EstadoAsistencia.CORREGIDA);
        asistencia.setJustificacionCorreccion(motivo);
        asistencia.setCorregidoPor(corrector);
        asistencia.setFechaCorreccion(LocalDateTime.now());
        asistenciaDAO.saveAndFlush(asistencia);

        RegistroAuditoria.registrar("CORREGIR", "ASISTENCIA", codigoAsistencia,
                correctorCorreo, "justificacion=" + motivo);
    }

    public long contarPresentes(Long codigoActividad) {
        Actividad actividad = actividadDAO.findById(codigoActividad)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        return asistenciaDAO.countByActividadAndEstado(actividad, EstadoAsistencia.PRESENTE);
    }
}
