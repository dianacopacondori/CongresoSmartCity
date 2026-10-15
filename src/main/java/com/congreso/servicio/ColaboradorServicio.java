package com.congreso.servicio;

import com.congreso.dao.ColaboradorDAO;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.enums.RolUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ColaboradorServicio {

    private final ColaboradorDAO colaboradorDAO;

    @Autowired
    public ColaboradorServicio(ColaboradorDAO colaboradorDAO) {
        this.colaboradorDAO = colaboradorDAO;
    }

    public Colaborador registrarColaborador(Colaborador colaborador) {
        return colaboradorDAO.save(colaborador);
    }

    public List<Colaborador> listarColaboradores() {
        return colaboradorDAO.findAll();
    }

    public List<Colaborador> listarPonentes() {
        return colaboradorDAO.findByTipoColaborador(RolUsuario.PONENTE);
    }

    public List<Colaborador> listarInstructores() {
        return colaboradorDAO.findByTipoColaborador(RolUsuario.INSTRUCTOR);
    }

    public Colaborador actualizarColaborador(Colaborador colaborador) {
        return colaboradorDAO.save(colaborador);
    }
}
