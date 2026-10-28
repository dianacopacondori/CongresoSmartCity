package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.enums.ModalidadPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsistenteDAO extends JpaRepository<Asistente, Long> {

    List<Asistente> findByModalidadPagoPreferida(ModalidadPago modalidad);

    List<Asistente> findByUniversidad(String universidad);

    /**
     * Búsqueda parcial por nombres o apellidos (case-insensitive).
     */
    @Query("SELECT a FROM Asistente a WHERE LOWER(a.nombres) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
            "OR LOWER(a.apellidos) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Asistente> buscarPorNombre(@Param("nombre") String nombre);
}
