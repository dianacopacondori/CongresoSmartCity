package com.congreso.servicio;

import com.congreso.dao.AsistenteDAO;
import com.congreso.dao.InscripcionDAO;
import com.congreso.dao.PagoDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.ModalidadPago;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Pago registrarPago(Long codigoAsistente, double importe, ModalidadPago modalidad, int cuotas, Long codigoInscripcion) {
        Asistente asistente = asistenteDAO.findById(codigoAsistente)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        Inscripcion inscripcion = inscripcionDAO.findById(codigoInscripcion)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        Pago pago = new Pago(asistente, importe, modalidad, cuotas);
        pago.setInscripcion(inscripcion);
        return pagoDAO.save(pago);
    }

    public boolean registrarAbono(Long codigoPago, double monto) {
        Pago pago = pagoDAO.findById(codigoPago)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
        pago.registrarPago(monto);
        pagoDAO.save(pago);
        return pago.estaPagado();
    }

    public Double consultarSaldoTotal(Long codigoAsistente) {
        Asistente asistente = asistenteDAO.findById(codigoAsistente)
                .orElseThrow(() -> new IllegalArgumentException("Asistente no encontrado"));
        return pagoDAO.calcularSaldoTotal(asistente);
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
}
