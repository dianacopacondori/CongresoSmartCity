package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ColaboradorDAO extends JpaRepository<Colaborador, Long>, DAO<Colaborador, Long> {

    List<Colaborador> findByTipoColaborador(RolUsuario tipo);

    List<Colaborador> findByInstitucion(String institucion);
}
