package com.congreso.controlador;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.TokenRecuperacion;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.RecuperacionServicio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Ejecutar explícitamente con -Dtest=RecuperacionPostgresIT. Datos ficticios y rollback. */
@SpringBootTest
@ActiveProfiles({"dev", "local"})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false", "spring.sql.init.mode=never"
})
@Transactional
class RecuperacionPostgresIT {
    @Autowired private RecuperacionServicio recuperacionServicio;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void tokenSeConsumeYContrasenaHashSePersisteEnPostgresql() throws Exception {
        try (var conexion = dataSource.getConnection()) {
            assertEquals("PostgreSQL", conexion.getMetaData().getDatabaseProductName());
        }

        Asistente asistente = new Asistente();
        asistente.setNombres("Prueba");
        asistente.setApellidos("Recuperacion");
        asistente.setCorreo("recuperacion-" + UUID.randomUUID() + "@example.invalid");
        asistente.setIdentificacion(UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        asistente.setContrasena(passwordEncoder.encode("anterior123"));
        asistente.setRol(RolUsuario.ASISTENTE);
        em.persist(asistente);
        em.flush();

        UUID valorToken = UUID.randomUUID();
        em.persist(new TokenRecuperacion(valorToken, asistente.getCodigo(), LocalDateTime.now()));
        em.flush();
        Long usuarioId = asistente.getCodigo();
        em.clear();

        assertTrue(recuperacionServicio.restablecer(valorToken, "nuevaClave123"));
        em.flush();
        em.clear();

        Usuario actualizado = em.find(Usuario.class, usuarioId);
        TokenRecuperacion consumido = em.find(TokenRecuperacion.class, valorToken);
        assertTrue(passwordEncoder.matches("nuevaClave123", actualizado.getContrasena()));
        assertTrue(consumido.isUsado());
    }
}
