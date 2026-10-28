package com.congreso.servicio;

import com.congreso.dao.FotocheckDAO;
import com.congreso.modelo.Fotocheck;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.EstadoFotocheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la generación y gestión de fotochecks.
 * Aplica el principio de responsabilidad única (SRP) de SOLID.
 *
 * @author Equipo de Desarrollo
 * @version 1.0.0
 */
@Service
@Transactional
public class FotocheckServicio {

    private final FotocheckDAO fotocheckDAO;

    @Autowired
    public FotocheckServicio(FotocheckDAO fotocheckDAO) {
        this.fotocheckDAO = fotocheckDAO;
    }

    /**
     * Genera un fotocheck para un usuario.
     *
     * @param usuario el usuario para quien se genera el fotocheck
     * @return el fotocheck generado
     */
    public Fotocheck generarFotocheck(Usuario usuario) {
        // Verificar si ya existe un fotocheck
        Optional<Fotocheck> existente = fotocheckDAO.findByUsuario(usuario);

        if (existente.isPresent()) {
            // Si existe y está emitido, retornarlo
            if (existente.get().getEstado() == EstadoFotocheck.EMITIDO) {
                return existente.get();
            }
            // Si existe pero está anulado, crear uno nuevo
        }

        Fotocheck fotocheck = new Fotocheck(usuario);
        return fotocheckDAO.save(fotocheck);
    }

    /**
     * Busca un fotocheck por su código.
     *
     * @param codigo el código del fotocheck
     * @return un Optional con el fotocheck encontrado, o vacío si no existe
     */
    public Optional<Fotocheck> buscarPorCodigo(Long codigo) {
        return fotocheckDAO.findByCodigo(codigo);
    }

    /**
     * Busca un fotocheck por usuario.
     *
     * @param usuario el usuario
     * @return un Optional con el fotocheck encontrado, o vacío si no existe
     */
    public Optional<Fotocheck> buscarPorUsuario(Usuario usuario) {
        return fotocheckDAO.findByUsuario(usuario);
    }

    /**
     * Anula un fotocheck.
     *
     * @param fotocheckId el código del fotocheck a anular
     * @return el fotocheck anulado
     * @throws IllegalArgumentException si el fotocheck no existe
     */
    public Fotocheck anularFotocheck(Long fotocheckId) {
        Fotocheck fotocheck = fotocheckDAO.findById(fotocheckId)
                .orElseThrow(() -> new IllegalArgumentException("Fotocheck no encontrado"));

        fotocheck.setEstado(EstadoFotocheck.ANULADO);
        return fotocheckDAO.save(fotocheck);
    }

    /**
     * Obtiene todos los fotochecks emitidos.
     *
     * @return una lista de fotochecks emitidos
     */
    public List<Fotocheck> obtenerFotochecksEmitidos() {
        return fotocheckDAO.findByEstado(EstadoFotocheck.EMITIDO);
    }

    /**
     * Valida un fotocheck para control de acceso.
     *
     * @param codigo el código del fotocheck
     * @return true si el fotocheck es válido y está emitido, false en caso contrario
     */
    public boolean validarFotocheck(Long codigo) {
        Optional<Fotocheck> fotocheckOpt = fotocheckDAO.findByCodigo(codigo);

        if (fotocheckOpt.isPresent()) {
            Fotocheck fotocheck = fotocheckOpt.get();
            return fotocheck.getEstado() == EstadoFotocheck.EMITIDO;
        }

        return false;
    }

    /**
     * Genera fotochecks masivos para todos los asistentes de un evento.
     *
     * @param listaUsuarios lista de usuarios para quienes se generarán fotochecks
     * @return cantidad de fotochecks generados
     */
    public int generarFotochecksMasivos(List<Usuario> listaUsuarios) {
        int contador = 0;

        for (Usuario usuario : listaUsuarios) {
            try {
                generarFotocheck(usuario);
                contador++;
            } catch (Exception e) {
                // Continuar con el siguiente usuario si hay un error
                // En una implementación real, se debería registrar el error con Logback
            }
        }

        return contador;
    }
}
