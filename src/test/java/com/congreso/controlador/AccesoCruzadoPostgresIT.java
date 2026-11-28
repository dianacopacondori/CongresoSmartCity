package com.congreso.controlador;

import com.congreso.modelo.*;
import com.congreso.modelo.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Comprueba que agenda y asistencia se resuelven desde la identidad propia, no desde IDs del cliente. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "local"})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never"
})
@Transactional
class AccesoCruzadoPostgresIT {

    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;

    @Test
    void asistenteSoloConsultaYSoloPuedeMutarSusDatos() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
        }

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        Asistente titular = asistente("titular", suffix);
        Asistente ajeno = asistente("ajeno", suffix);
        Colaborador ponente = ponente(suffix);
        Registro titularData = registroPagado(titular, ponente, suffix, "TITULAR");
        Registro ajenoData = registroPagado(ajeno, ponente, suffix, "AJENO");
        em.flush();

        MockHttpSession sessionTitular = new MockHttpSession();
        sessionTitular.setAttribute("usuario", titular);
        var usuarioTitular = SecurityMockMvcRequestPostProcessors.user(titular.getCorreo()).roles("ASISTENTE");

        mvc.perform(get("/inscripciones/mi-agenda").with(usuarioTitular).session(sessionTitular))
                .andExpect(status().isOk())
                .andExpect(model().attribute("actividades", hasSize(1)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(titularData.actividad().getTitulo())))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(ajenoData.actividad().getTitulo()))));

        mvc.perform(get("/asistencias/mis-asistencias").with(usuarioTitular).session(sessionTitular))
                .andExpect(status().isOk())
                .andExpect(model().attribute("asistencias", hasSize(1)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(titularData.actividad().getTitulo())))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(ajenoData.actividad().getTitulo()))));

        mvc.perform(post("/inscripciones/agregar-actividad").with(usuarioTitular).with(csrf())
                        .param("codigoInscripcion", ajenoData.inscripcion().getCodigo().toString())
                        .param("codigoActividad", titularData.actividad().getCodigo().toString()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/pagos/abonar/{pagoId}", ajenoData.pago().getCodigoPago())
                        .with(usuarioTitular).with(csrf()))
                .andExpect(status().isForbidden());

        em.flush();
        em.clear();
        Inscripcion inscripcionAjena = em.find(Inscripcion.class, ajenoData.inscripcion().getCodigo());
        assertEquals(1, inscripcionAjena.getActividades().size());
        Pago pagoAjeno = em.find(Pago.class, ajenoData.pago().getCodigoPago());
        assertEquals(EstadoPago.PAGADO, pagoAjeno.getEstado());
        assertEquals(0.0, pagoAjeno.getSaldo());
        assertEquals(1L, em.createQuery(
                        "SELECT COUNT(a) FROM Asistencia a WHERE a.asistente.codigo = :id", Long.class)
                .setParameter("id", ajeno.getCodigo())
                .getSingleResult());
    }

    private Asistente asistente(String nombre, String suffix) {
        Asistente asistente = new Asistente();
        asistente.setNombres("Prueba " + nombre);
        asistente.setApellidos("M13");
        asistente.setIdentificacion(nombre.substring(0, 1).toUpperCase() + suffix);
        asistente.setCorreo("m13-" + nombre + "-" + suffix + "@example.invalid");
        asistente.setContrasena("hash-de-prueba-no-real");
        asistente.setRol(RolUsuario.ASISTENTE);
        em.persist(asistente);
        return asistente;
    }

    private Colaborador ponente(String suffix) {
        Colaborador ponente = new Colaborador();
        ponente.setNombres("Ponente");
        ponente.setApellidos("M13");
        ponente.setIdentificacion("P" + suffix);
        ponente.setCorreo("m13-ponente-" + suffix + "@example.invalid");
        ponente.setContrasena("hash-de-prueba-no-real");
        ponente.setRol(RolUsuario.PONENTE);
        ponente.setTipoColaborador(RolUsuario.PONENTE);
        em.persist(ponente);
        return ponente;
    }

    private Registro registroPagado(Asistente asistente, Colaborador ponente, String suffix, String label) {
        Sala sala = new Sala("Sala " + label + suffix, 10, "Pruebas M13");
        em.persist(sala);

        Ponencia actividad = new Ponencia();
        actividad.setTitulo("Actividad " + label + suffix);
        actividad.setSala(sala);
        actividad.setPonente(ponente);
        actividad.setFecha(LocalDate.now().plusDays(3));
        actividad.setHorarioInicio(LocalTime.of(9, 0));
        actividad.setHorarioFin(LocalTime.of(10, 0));
        actividad.setCupo(10);
        em.persist(actividad);
        em.flush();

        Inscripcion inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.confirmar();
        inscripcion.agregarActividad(actividad);
        em.persist(inscripcion);

        Pago pago = new Pago(asistente, 50, ModalidadPago.COMPLETO, 1);
        pago.setInscripcion(inscripcion);
        pago.setEstado(EstadoPago.PAGADO);
        pago.setSaldo(0);
        em.persist(pago);

        Asistencia asistencia = new Asistencia();
        asistencia.setAsistente(asistente);
        asistencia.setActividad(actividad);
        asistencia.setFecha(actividad.getFecha());
        asistencia.setHora(LocalTime.of(9, 30));
        asistencia.setMecanismoValidacion("MANUAL");
        asistencia.setEstado(EstadoAsistencia.PRESENTE);
        em.persist(asistencia);
        return new Registro(inscripcion, pago, actividad);
    }

    private record Registro(Inscripcion inscripcion, Pago pago, Ponencia actividad) {}
}
