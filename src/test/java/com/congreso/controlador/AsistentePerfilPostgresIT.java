package com.congreso.controlador;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.enums.EstadoUsuario;
import com.congreso.modelo.enums.RolUsuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Se ejecuta explícitamente con -Dtest=AsistentePerfilPostgresIT; requiere PostgreSQL local. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never"
})
@Transactional // MockMvc síncrono participa en esta transacción; cada prueba hace rollback.
class AsistentePerfilPostgresIT {
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager entityManager;
    @Autowired private DataSource dataSource;

    @Test
    void utilizaPostgresqlReal() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
        }
    }

    @Test
    void postManipuladoSoloActualizaLosDatosPermitidosDeLaCuentaAutenticada() throws Exception {
        Asistente propia = crearAsistente("Propia");
        Asistente ajena = crearAsistente("Ajena");
        Long idPropia = propia.getCodigo();
        Long idAjena = ajena.getCodigo();
        String correoPropio = propia.getCorreo();
        String identificacionPropia = propia.getIdentificacion();
        String contrasenaPropia = propia.getContrasena();
        entityManager.flush();
        entityManager.clear();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuario", ajena);
        mvc.perform(post("/asistentes/mi-perfil")
                        .with(user(correoPropio).roles("ASISTENTE")).with(csrf()).session(session)
                        .param("nombres", "Nombre nuevo").param("apellidos", "Apellido nuevo")
                        .param("telefono", "999888777")
                        .param("codigo", idAjena.toString()).param("correo", ajena.getCorreo())
                        .param("identificacion", "00000000")
                        .param("contrasena", "contraseña inyectada")
                        .param("rol", "ADMINISTRADOR").param("estado", "SUSPENDIDO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/asistentes/mi-perfil"))
                .andExpect(flash().attributeExists("mensaje"));

        entityManager.flush();
        entityManager.clear();
        Asistente actualizada = entityManager.find(Asistente.class, idPropia);
        Asistente sinCambios = entityManager.find(Asistente.class, idAjena);
        assertEquals("Nombre nuevo", actualizada.getNombres());
        assertEquals("Apellido nuevo", actualizada.getApellidos());
        assertEquals("999888777", actualizada.getTelefono());
        assertEquals(correoPropio, actualizada.getCorreo());
        assertEquals(identificacionPropia, actualizada.getIdentificacion());
        assertEquals(contrasenaPropia, actualizada.getContrasena());
        assertEquals(RolUsuario.ASISTENTE, actualizada.getRol());
        assertEquals(EstadoUsuario.ACTIVO, actualizada.getEstado());
        assertEquals("Ajena", sinCambios.getNombres());
        assertNotEquals(idPropia, idAjena);
    }

    @Test
    void datosInvalidosNoSePersisten() throws Exception {
        Asistente propia = crearAsistente("Original");
        Long id = propia.getCodigo();
        String correo = propia.getCorreo();
        entityManager.flush();
        entityManager.clear();

        mvc.perform(post("/asistentes/mi-perfil")
                        .with(user(correo).roles("ASISTENTE")).with(csrf())
                        .param("nombres", " ").param("apellidos", "Valido").param("edad", "15"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("perfil", "nombres", "edad"));

        entityManager.flush();
        entityManager.clear();
        assertEquals("Original", entityManager.find(Asistente.class, id).getNombres());
    }

    private Asistente crearAsistente(String nombre) {
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        Asistente asistente = new Asistente();
        asistente.setNombres(nombre);
        asistente.setApellidos("Prueba");
        asistente.setCorreo("c01-" + sufijo + "@example.invalid");
        asistente.setIdentificacion(sufijo);
        asistente.setContrasena("hash-de-prueba-no-real");
        asistente.setRol(RolUsuario.ASISTENTE);
        entityManager.persist(asistente);
        return asistente;
    }
}
