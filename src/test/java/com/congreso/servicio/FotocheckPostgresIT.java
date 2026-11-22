package com.congreso.servicio;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.Fotocheck;
import com.congreso.modelo.enums.EstadoFotocheck;
import com.congreso.modelo.enums.RolUsuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ejecutar explícitamente con -Dtest=FotocheckPostgresIT; datos ficticios con rollback. */
@SpringBootTest
@Transactional
class FotocheckPostgresIT {

    @Autowired private FotocheckServicio servicio;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;

    @Test
    void emiteYRenuevaElMismoRegistroEnPostgres() throws Exception {
        try (var conexion = dataSource.getConnection()) {
            assertTrue(conexion.getMetaData().getDatabaseProductName().contains("PostgreSQL"));
        }

        String clave = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        Asistente asistente = new Asistente();
        asistente.setNombres("Prueba");
        asistente.setApellidos("Fotocheck");
        asistente.setCorreo("fotocheck-" + clave + "@example.invalid");
        asistente.setIdentificacion(clave);
        asistente.setContrasena("hash-de-prueba-no-real");
        asistente.setRol(RolUsuario.ASISTENTE);
        em.persist(asistente);
        em.flush();

        Fotocheck emitido = servicio.generarFotocheck(asistente);
        em.flush();
        Long codigoOriginal = emitido.getCodigo();

        servicio.anularFotocheck(codigoOriginal);
        Fotocheck renovado = servicio.generarFotocheck(asistente);
        em.flush();

        assertEquals(codigoOriginal, renovado.getCodigo());
        assertEquals(EstadoFotocheck.EMITIDO, renovado.getEstado());
        Long total = em.createQuery("SELECT COUNT(f) FROM Fotocheck f WHERE f.usuario = :u", Long.class)
                .setParameter("u", asistente).getSingleResult();
        assertEquals(1L, total);
    }
}
