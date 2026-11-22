package com.congreso.dao;

import com.congreso.modelo.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteDAO extends JpaRepository<Reporte, Long> {

    List<Reporte> findAllByOrderByFechaGeneracionDesc();
}
