package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ColaboradorDAO extends JpaRepository<Colaborador, Long> {

    List<Colaborador> findByTipoColaborador(RolUsuario tipo);

    List<Colaborador> findByTipoColaboradorAndEstado(RolUsuario tipo, EstadoUsuario estado);

    List<Colaborador> findByInstitucion(String institucion);

    Optional<Colaborador> findByCorreoIgnoreCase(String correo);
}
