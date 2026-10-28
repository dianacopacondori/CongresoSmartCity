package com.congreso.servicio;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class PagoServicio {

    private final PagoDAO pagoDAO;
    private final AsistenteDAO asistenteDAO;
    private final InscripcionDAO inscripcionDAO;

    @Autowired
    public PagoServicio(PagoDAO pagoDAO, AsistenteDAO asistenteDAO, InscripcionDAO inscripcionDAO) {
        this.pagoDAO = pagoDAO;
        this.asistenteDAO = asistenteDAO;
        this.inscripcionDAO = inscripcionDAO;
    }

    /**
     * Registra un pago recibiendo los datos desglosados.
     */
    public Pago registrarPago(Long codigoAsistente, double importe, ModalidadPago modalidad, int cuotas, Long codigoInscripcion) {
        Asistente asistente = asistenteDAO.findById(codigoAsistente)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        Inscripcion inscripcion = inscripcionDAO.findById(codigoInscripcion)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        Pago pago = new Pago(asistente, importe, modalidad, cuotas);
        return pagoDAO.save(pago);
    }

    /**
     * NUEVO: Método sobrecargado que recibe el objeto Pago directamente desde el Servlet.
     */
    public Pago registrarPago(Pago pago) {
        return pagoDAO.save(pago);
    }

    /**
     * Registra un abono o cuota a un pago existente.
     */
    public boolean registrarAbono(Long codigoPago, double monto) {
        Pago pago = pagoDAO.findById(codigoPago)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
        pago.registrarPago(monto); // Este método ya existe en tu entidad Pago
        pagoDAO.save(pago);
        return pago.estaPagado();
    }

    /**
     * NUEVO: Alias para que el Servlet pueda llamarlo con el nombre "registrarCuota".
     */
    public void registrarCuota(Long codigoPago, double monto) {
        registrarAbono(codigoPago, monto);
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

    /**
     * NUEVO: Busca pagos por estado (usado por el filtro del admin).
     */
    public List<Pago> buscarPorEstado(String estado) {
        try {
            EstadoPago estadoEnum = EstadoPago.valueOf(estado.toUpperCase());
            return pagoDAO.findByEstadoIn(Collections.singletonList(estadoEnum));
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    /**
     * NUEVO: Obtiene todos los pagos del sistema (para el admin).
     */
    public List<Pago> obtenerTodosLosPagos() {
        return pagoDAO.findAll();
    }
}