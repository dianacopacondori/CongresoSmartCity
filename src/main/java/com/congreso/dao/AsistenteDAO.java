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
public interface AsistenteDAO extends JpaRepository<Asistente, Long>, DAO<Asistente, Long> {

    List<Asistente> findByModalidadPagoPreferida(ModalidadPago modalidad);

    List<Asistente> findByUniversidad(String universidad);

    List<Asistente> findByEmpresa(String empresa);

    @Query("SELECT a FROM Asistente a WHERE a.edad BETWEEN :min AND :max")
    List<Asistente> buscarPorRangoEdad(@Param("min") int min, @Param("max") int max);
}
