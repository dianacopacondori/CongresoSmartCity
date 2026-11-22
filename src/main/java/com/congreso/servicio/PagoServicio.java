package com.congreso.servicio;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.dto.EstadoParticipacion;
import com.congreso.dto.EstadoParticipacion.Paso;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.modelo.enums.RolUsuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class PagoServicio {
    private static final double TARIFA_ESTUDIANTE = 50.0;
    private static final double TARIFA_EXTERNO = 100.0;
    private static final double CUOTA_ESTUDIANTE = 25.0;

    private final PagoDAO pagoDAO;
    private final AsistenteDAO asistenteDAO;
    private final InscripcionDAO inscripcionDAO;

    public PagoServicio(PagoDAO pagoDAO, AsistenteDAO asistenteDAO, InscripcionDAO inscripcionDAO) {
        this.pagoDAO = pagoDAO;
        this.asistenteDAO = asistenteDAO;
        this.inscripcionDAO = inscripcionDAO;
    }

    public Pago simularPago(String correoAutenticado, Long codigoInscripcion, ModalidadPago modalidad) {
        Asistente asistente = asistentePropio(correoAutenticado);
        Inscripcion inscripcion = inscripcionPropia(asistente, codigoInscripcion);
        if (inscripcion.getEstado() != EstadoInscripcion.PENDIENTE || inscripcion.isConfirmacion()) {
            throw new IllegalStateException("La inscripción no está pendiente de pago");
        }
        if (pagoDAO.findByInscripcion(inscripcion).isPresent()) {
            throw new IllegalStateException("Esta inscripción ya tiene un pago simulado");
        }
        double importe = tarifa(inscripcion);
        if (modalidad == null || (modalidad == ModalidadPago.CUOTAS
                && !"Estudiante".equals(inscripcion.getTipo()))) {
            throw new IllegalArgumentException("Modalidad de pago no permitida para esta inscripción");
        }

        Pago pago = new Pago(asistente, importe, modalidad, modalidad == ModalidadPago.CUOTAS ? 2 : 1);
        pago.setInscripcion(inscripcion);
        pago.setObservaciones("SIMULACIÓN ACADÉMICA: no se realizó ningún cobro real");
        double primerAbono = modalidad == ModalidadPago.CUOTAS ? CUOTA_ESTUDIANTE : importe;
        pago.registrarPago(primerAbono);
        if (modalidad != ModalidadPago.CUOTAS) {
            inscripcion.confirmar();
        }
        Pago guardado = pagoDAO.saveAndFlush(pago);
        inscripcionDAO.saveAndFlush(inscripcion);
        return guardado;
    }

    public Pago simularSegundaCuota(String correoAutenticado, Long codigoPago) {
        Asistente asistente = asistentePropio(correoAutenticado);
        Pago pagoInicial = pagoDAO.findById(codigoPago)
                .orElseThrow(() -> new IllegalArgumentException("Pago simulado no encontrado"));
        Inscripcion inscripcion = inscripcionPropia(asistente, pagoInicial.getInscripcion().getCodigo());
        Pago pago = pagoDAO.findByInscripcion(inscripcion)
                .orElseThrow(() -> new IllegalArgumentException("Pago simulado no encontrado"));
        if (!pago.getCodigoPago().equals(codigoPago) || !pago.getAsistente().getCodigo().equals(asistente.getCodigo())) {
            throw new AccessDeniedException("Pago ajeno al asistente autenticado");
        }
        if (inscripcion.getEstado() != EstadoInscripcion.PENDIENTE || inscripcion.isConfirmacion()
                || !"Estudiante".equals(inscripcion.getTipo()) || pago.getModalidad() != ModalidadPago.CUOTAS
                || pago.getEstado() != EstadoPago.PARCIAL || pago.getSaldo() != CUOTA_ESTUDIANTE
                || pago.getImporte() != TARIFA_ESTUDIANTE || pago.getCuotas() != 2) {
            throw new IllegalStateException("Este pago no tiene una segunda cuota disponible");
        }
        pago.registrarPago(CUOTA_ESTUDIANTE);
        inscripcion.confirmar();
        pagoDAO.saveAndFlush(pago);
        inscripcionDAO.saveAndFlush(inscripcion);
        return pago;
    }

    @Transactional(readOnly = true)
    public List<Inscripcion> inscripcionesPendientesPropias(String correoAutenticado) {
        Asistente asistente = asistentePropio(correoAutenticado);
        return inscripcionDAO.findByAsistente(asistente).stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.PENDIENTE && !i.isConfirmacion())
                .filter(i -> pagoDAO.findByInscripcion(i).isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Pago> pagosPropios(String correoAutenticado) {
        return pagoDAO.findByAsistente(asistentePropio(correoAutenticado));
    }

    @Transactional(readOnly = true)
    public EstadoParticipacion estadoParticipacion(String correoAutenticado) {
        Asistente asistente = asistentePropio(correoAutenticado);
        List<Inscripcion> inscripciones = inscripcionDAO.findByAsistente(asistente);
        // Preferir una inscripción pendiente que requiera acción. No depender del orden del DAO.
        Inscripcion actual = inscripciones.stream()
                .filter(i -> i.getEstado() == EstadoInscripcion.PENDIENTE)
                .max(Comparator.comparing(Inscripcion::getCodigo))
                .or(() -> inscripciones.stream()
                        .filter(i -> i.getEstado() == EstadoInscripcion.CONFIRMADA)
                        .max(Comparator.comparing(Inscripcion::getCodigo)))
                .orElse(null);
        if (actual == null) {
            return new EstadoParticipacion("SIN INSCRIPCIÓN", "SIN PAGOS", Paso.CREAR_INSCRIPCION, null);
        }
        Pago pago = pagoDAO.findByAsistente(asistente).stream()
                .filter(p -> p.getInscripcion() != null
                        && actual.getCodigo().equals(p.getInscripcion().getCodigo()))
                .max(Comparator.comparing(Pago::getCodigoPago))
                .orElse(null);
        String estadoPago = pago == null ? "SIN PAGOS" : pago.getEstado().name();
        Paso paso;
        if (actual.getEstado() == EstadoInscripcion.CONFIRMADA) {
            paso = pago != null && pago.getEstado() == EstadoPago.PAGADO
                    ? Paso.VER_AGENDA : Paso.REVISAR_ESTADO;
        } else if (pago == null) {
            paso = Paso.SIMULAR_PAGO;
        } else if (pago.getEstado() == EstadoPago.PARCIAL
                && pago.getModalidad() == ModalidadPago.CUOTAS && pago.getSaldo() > 0) {
            paso = Paso.COMPLETAR_CUOTA;
        } else {
            paso = Paso.REVISAR_ESTADO;
        }
        return new EstadoParticipacion(actual.getEstado().name(), estadoPago, paso, actual.getCodigo());
    }

    private Asistente asistentePropio(String correoAutenticado) {
        Asistente asistente = asistenteDAO.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new AccessDeniedException("Asistente no disponible"));
        if (!asistente.estaActivo() || asistente.getRol() != RolUsuario.ASISTENTE) {
            throw new AccessDeniedException("Asistente no disponible");
        }
        return asistente;
    }

    private Inscripcion inscripcionPropia(Asistente asistente, Long codigo) {
        Inscripcion inscripcion = inscripcionDAO.bloquearPorCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
        if (!inscripcion.getAsistente().getCodigo().equals(asistente.getCodigo())) {
            throw new AccessDeniedException("Inscripción ajena al asistente autenticado");
        }
        return inscripcion;
    }

    private double tarifa(Inscripcion inscripcion) {
        if (inscripcion.getTipo() == null) {
            throw new IllegalArgumentException("Tipo de inscripción no permitido para pagos simulados");
        }
        return switch (inscripcion.getTipo()) {
            case "Estudiante" -> TARIFA_ESTUDIANTE;
            case "Externo" -> TARIFA_EXTERNO;
            default -> throw new IllegalArgumentException("Tipo de inscripción no permitido para pagos simulados");
        };
    }

    public Double consultarSaldoTotal(Long codigoAsistente) {
        Asistente asistente = asistenteDAO.findById(codigoAsistente)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        Double saldo = pagoDAO.calcularSaldoTotal(asistente);
        return saldo != null ? saldo : 0.0;
    }

    public List<Pago> listarPagosPendientes(Long codigoAsistente) {
        Asistente asistente = asistenteDAO.findById(codigoAsistente)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        return pagoDAO.buscarPendientesPorAsistente(asistente);
    }

    public List<Pago> listarTodosLosPagos(Long codigoAsistente) {
        Asistente asistente = asistenteDAO.findById(codigoAsistente)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        return pagoDAO.findByAsistente(asistente);
    }

    public List<Pago> buscarPorEstado(String estado) {
        try {
            EstadoPago estadoEnum = EstadoPago.valueOf(estado.toUpperCase());
            return pagoDAO.findByEstadoIn(Collections.singletonList(estadoEnum));
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    public List<Pago> obtenerTodosLosPagos() {
        return pagoDAO.findAll();
    }

    /** Importe cubierto por pagos pagados y parciales (simulación académica). */
    public double ingresosCubiertos() {
        Double total = pagoDAO.calcularIngresosCubiertos();
        return total == null ? 0.0 : total;
    }

    /** Pagos pendientes o parciales, es decir, con saldo por cubrir. */
    public long contarPagosPendientes() {
        return pagoDAO.countByEstado(EstadoPago.PENDIENTE) + pagoDAO.countByEstado(EstadoPago.PARCIAL);
    }

    /** Últimos pagos para el panel de administración, sin depender del orden de inserción. */
    public List<Pago> obtenerPagosRecientes(int limite) {
        Comparator<Pago> masRecientePrimero = Comparator
                .comparing(Pago::getFechaPago, Comparator.reverseOrder())
                .thenComparing(Pago::getCodigoPago, Comparator.nullsLast(Comparator.reverseOrder()));
        return pagoDAO.findAll().stream()
                .sorted(masRecientePrimero)
                .limit(limite)
                .toList();
    }
}
