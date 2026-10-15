package com.congreso.servicio;

import com.congreso.dao.AsistenteDAO;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.enums.ModalidadPago;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AsistenteServicio {

    private final AsistenteDAO asistenteDAO;

    @Autowired
    public AsistenteServicio(AsistenteDAO asistenteDAO) {
        this.asistenteDAO = asistenteDAO;
    }

    public Asistente registrarAsistente(Asistente asistente) {
        return asistenteDAO.save(asistente);
    }

    public Asistente buscarPorCodigo(Long codigo) {
        return asistenteDAO.findById(codigo).orElse(null);
    }

    public List<Asistente> listarAsistentes() {
        return asistenteDAO.findAll();
    }

    public List<Asistente> buscarPorModalidadPago(ModalidadPago modalidad) {
        return asistenteDAO.findByModalidadPagoPreferida(modalidad);
    }

    public List<Asistente> buscarPorUniversidad(String universidad) {
        return asistenteDAO.findByUniversidad(universidad);
    }

    public Asistente actualizarAsistente(Asistente asistente) {
        return asistenteDAO.save(asistente);
    }

    public void eliminarAsistente(Long codigo) {
        asistenteDAO.deleteById(codigo);
    }
}
