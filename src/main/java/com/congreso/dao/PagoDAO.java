package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PagoDAO extends JpaRepository<Pago, Long>, DAO<Pago, Long> {

    List<Pago> findByAsistente(Asistente asistente);

    List<Pago> findByEstado(EstadoPago estado);

    List<Pago> findByModalidad(ModalidadPago modalidad);

    @Query("SELECT p FROM Pago p WHERE p.asistente = :asistente AND p.estado <> com.congreso.modelo.enums.EstadoPago.PAGADO")
    List<Pago> buscarPendientesPorAsistente(@Param("asistente") Asistente asistente);

    @Query("SELECT COALESCE(SUM(p.saldo), 0.0) FROM Pago p WHERE p.asistente = :asistente AND p.estado <> com.congreso.modelo.enums.EstadoPago.PAGADO")
    Double calcularSaldoTotal(@Param("asistente") Asistente asistente);

    @Query("SELECT p FROM Pago p WHERE p.fechaPago BETWEEN :inicio AND :fin")
    List<Pago> buscarPorRangoFechas(@Param("inicio") LocalDate inicio,
                                    @Param("fin") LocalDate fin);
}
