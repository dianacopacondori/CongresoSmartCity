package com.congreso.dao;

import com.congreso.interfaces.DAO;
import com.congreso.modelo.Fotocheck;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoFotocheck;
import com.congreso.modelo.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FotocheckDAO extends JpaRepository<Fotocheck, Long>, DAO<Fotocheck, Long> {

    Optional<Fotocheck> findByUsuario(Usuario usuario);

    List<Fotocheck> findByEstado(EstadoFotocheck estado);

    List<Fotocheck> findByTipoUsuario(RolUsuario tipoUsuario);
}
