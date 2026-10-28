package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoDAO extends JpaRepository<Pago, Long>{

    List<Pago> findByAsistenteOrderByFechaPagoDesc(Asistente asistente);

    // Alias solicitado por el servicio
    default List<Pago> findByAsistente(Asistente asistente) {
        return findByAsistenteOrderByFechaPagoDesc(asistente);
    }

    List<Pago> findByEstadoIn(List<EstadoPago> estados);

    List<Pago> findByFechaPagoBetween(LocalDate fechaInicio, LocalDate fechaFin);

    Optional<Pago> findByCodigoPago(Long codigoPago);

    // Método solicitado por PagoServicio: Suma los saldos pendientes o parciales
    @Query("SELECT COALESCE(SUM(p.saldo), 0.0) FROM Pago p WHERE p.asistente = :asistente AND p.estado IN ('PENDIENTE', 'PARCIAL')")
    Double calcularSaldoTotal(@Param("asistente") Asistente asistente);

    // Método solicitado por PagoServicio: Filtra solo los pagos no completados
    default List<Pago> buscarPendientesPorAsistente(Asistente asistente) {
        return findByAsistente(asistente).stream()
                .filter(p -> p.getEstado() == EstadoPago.PENDIENTE || p.getEstado() == EstadoPago.PARCIAL)
                .toList();
    }
}
