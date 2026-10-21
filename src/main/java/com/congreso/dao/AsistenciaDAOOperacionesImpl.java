package com.congreso.dao;

import com.congreso.modelo.Asistencia;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class AsistenciaDAOOperacionesImpl implements AsistenciaDAOOperaciones {

    private static final Logger log = LoggerFactory.getLogger(AsistenciaDAOOperacionesImpl.class);

    private final EntityManager entityManager;

    public AsistenciaDAOOperacionesImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Asistencia guardarConRegistro(Asistencia asistencia) {
        try {
            Asistencia guardada;
            if (asistencia.getCodigo() == null) {
                entityManager.persist(asistencia);
                guardada = asistencia;
            } else {
                guardada = entityManager.merge(asistencia);
            }
            entityManager.flush();
            log.info("Asistencia guardada correctamente. id={}", guardada.getCodigo());
            return guardada;
        } catch (PersistenceException exception) {
            log.error("Error de persistencia al guardar la asistencia", exception);
            throw exception;
        }
    }
}
