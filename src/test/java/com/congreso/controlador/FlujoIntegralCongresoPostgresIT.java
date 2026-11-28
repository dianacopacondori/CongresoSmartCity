package com.congreso.controlador;

import com.congreso.modelo.*;
import com.congreso.modelo.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Recorre el flujo de participación sobre PostgreSQL y revierte los datos al terminar. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "local"})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never"
})
@Transactional
class FlujoIntegralCongresoPostgresIT {

    private static final String PASSWORD = "ClavePrueba-2026";

    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;

    @Test
    void registroLoginPagoAgendaFotocheckAsistenciaYReporteExportable() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
        }

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String email = "m13-" + suffix + "@example.invalid";

        mvc.perform(post("/usuarios/registro")
                        .param("nombres", "Participante")
                        .param("apellidos", "M13")
                        .param("identificacion", suffix)
                        .param("correo", email)
                        .param("contrasena", PASSWORD)
                        .param("edad", "24"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/usuarios/registro").with(csrf())
                        .param("nombres", "Participante")
                        .param("apellidos", "M13")
                        .param("identificacion", suffix)
                        .param("correo", email)
                        .param("contrasena", PASSWORD)
                        .param("edad", "24")
                        .param("procedencia", "Prueba de integración"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/login"))
                .andExpect(flash().attributeExists("mensaje"));

        mvc.perform(post("/usuarios/login").with(csrf())
                        .param("correo", email)
                        .param("contrasena", "incorrecta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/login?error"));

        MvcResult login = mvc.perform(post("/usuarios/login").with(csrf())
                        .param("correo", email)
                        .param("contrasena", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard-asistente"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        Asistente registrado = (Asistente) session.getAttribute("usuario");
        assertNotNull(registrado);
        assertEquals(email, registrado.getCorreo());

        Sala salaPonencia = persistirSala("M13 ponencia " + suffix);
        Sala salaTaller = persistirSala("M13 taller " + suffix);
        Colaborador ponente = persistirColaborador("ponente", suffix, RolUsuario.PONENTE);
        Colaborador instructor = persistirColaborador("instructor", suffix, RolUsuario.INSTRUCTOR);

        Ponencia ponencia = new Ponencia();
        ponencia.setTitulo("Ponencia M13 " + suffix);
        ponencia.setSala(salaPonencia);
        ponencia.setPonente(ponente);
        ponencia.setFecha(LocalDate.now().plusDays(7));
        ponencia.setHorarioInicio(LocalTime.of(9, 0));
        ponencia.setHorarioFin(LocalTime.of(10, 0));
        ponencia.setCupo(20);
        em.persist(ponencia);

        Taller taller = new Taller();
        taller.setTitulo("Taller M13 " + suffix);
        taller.setSala(salaTaller);
        taller.setInstructor(instructor);
        taller.setFecha(LocalDate.now().plusDays(7));
        taller.setHorarioInicio(LocalTime.of(11, 0));
        taller.setHorarioFin(LocalTime.of(12, 0));
        taller.setCupo(20);
        taller.setCosto(java.math.BigDecimal.ZERO);
        em.persist(taller);
        em.flush();

        mvc.perform(post("/inscripciones/nueva").with(csrf()).session(session)
                        .param("tipo", "Estudiante"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inscripciones/confirmacion"))
                .andExpect(flash().attributeExists("mensaje"));

        Inscripcion inscripcion = em.createQuery(
                        "SELECT i FROM Inscripcion i WHERE i.asistente.codigo = :asistente ORDER BY i.codigo DESC",
                        Inscripcion.class)
                .setParameter("asistente", registrado.getCodigo())
                .setMaxResults(1)
                .getSingleResult();
        Long inscripcionId = inscripcion.getCodigo();

        mvc.perform(post("/pagos/nuevo").with(csrf()).session(session)
                        .param("codigoInscripcion", inscripcionId.toString())
                        .param("modalidad", "COMPLETO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pagos/confirmacion"))
                .andExpect(flash().attributeExists("mensaje"));

        em.flush();
        em.clear();
        Inscripcion pagada = em.find(Inscripcion.class, inscripcionId);
        assertEquals(EstadoInscripcion.CONFIRMADA, pagada.getEstado());
        assertTrue(pagada.isConfirmacion());
        Pago pago = em.createQuery("SELECT p FROM Pago p WHERE p.inscripcion.codigo = :id", Pago.class)
                .setParameter("id", inscripcionId)
                .getSingleResult();
        assertEquals(EstadoPago.PAGADO, pago.getEstado());
        assertEquals(0.0, pago.getSaldo());

        for (Long actividadId : new Long[]{ponencia.getCodigo(), taller.getCodigo()}) {
            mvc.perform(post("/inscripciones/agregar-actividad").with(csrf()).session(session)
                            .param("codigoInscripcion", inscripcionId.toString())
                            .param("codigoActividad", actividadId.toString()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/inscripciones/mi-agenda"))
                    .andExpect(flash().attributeExists("mensaje"));
        }

        mvc.perform(get("/inscripciones/mi-agenda").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("actividades", hasSize(2)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(ponencia.getTitulo())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(taller.getTitulo())));

        MvcResult descargaFotocheck = mvc.perform(get("/fotocheck/descargar").session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")))
                .andReturn();
        byte[] pdf = descargaFotocheck.getResponse().getContentAsByteArray();
        assertTrue(new String(pdf, 0, Math.min(pdf.length, 8), StandardCharsets.ISO_8859_1)
                .startsWith("%PDF-1."));

        Fotocheck fotocheck = em.createQuery(
                        "SELECT f FROM Fotocheck f WHERE f.usuario.codigo = :id", Fotocheck.class)
                .setParameter("id", registrado.getCodigo())
                .getSingleResult();
        assertEquals(EstadoFotocheck.EMITIDO, fotocheck.getEstado());

        Administrador admin = persistirAdministrador(suffix);
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("usuario", admin);
        mvc.perform(post("/asistencias/registrar")
                        .with(user(admin.getCorreo()).roles("ADMINISTRADOR"))
                        .with(csrf())
                        .session(adminSession)
                        .param("codigoAsistente", registrado.getCodigo().toString())
                        .param("codigoActividad", ponencia.getCodigo().toString())
                        .param("mecanismoValidacion", "CODIGO")
                        .param("codigoFotocheck", fotocheck.getCodigo().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "Asistencia registrada y verificada"));

        em.flush();
        assertEquals(1L, em.createQuery(
                        "SELECT COUNT(a) FROM Asistencia a WHERE a.asistente.codigo = :asistente "
                                + "AND a.actividad.codigo = :actividad", Long.class)
                .setParameter("asistente", registrado.getCodigo())
                .setParameter("actividad", ponencia.getCodigo())
                .getSingleResult());

        mvc.perform(get("/reportes/admin/generar")
                        .with(user(admin.getCorreo()).roles("ADMINISTRADOR"))
                        .session(adminSession)
                        .param("tipo", "ASISTENCIAS")
                        .param("actividadId", ponencia.getCodigo().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("reporte"));

        MvcResult excel = mvc.perform(get("/reportes/admin/exportar-excel")
                        .with(user(admin.getCorreo()).roles("ADMINISTRADOR"))
                        .session(adminSession)
                        .param("tipo", "ASISTENCIAS")
                        .param("actividadId", ponencia.getCodigo().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString("spreadsheetml.sheet")))
                .andReturn();
        byte[] excelBytes = excel.getResponse().getContentAsByteArray();
        assertTrue(excelBytes.length > 4);
        assertEquals('P', excelBytes[0]);
        assertEquals('K', excelBytes[1]);

        em.flush();
        Reporte reporte = em.createQuery(
                        "SELECT r FROM Reporte r WHERE r.tipo = :tipo ORDER BY r.codigo DESC", Reporte.class)
                .setParameter("tipo", TipoReporte.ASISTENCIAS)
                .setMaxResults(1)
                .getSingleResult();
        assertTrue(reporte.getResultados().stream().anyMatch(fila -> fila.contains(email)));
    }

    private Sala persistirSala(String nombre) {
        Sala sala = new Sala(nombre, 30, "Pruebas M13");
        em.persist(sala);
        return sala;
    }

    private Colaborador persistirColaborador(String rolNombre, String suffix, RolUsuario rol) {
        Colaborador colaborador = new Colaborador();
        colaborador.setNombres("Responsable");
        colaborador.setApellidos(rolNombre);
        colaborador.setIdentificacion(rol.name().substring(0, 1) + suffix);
        colaborador.setCorreo("m13-" + rolNombre + "-" + suffix + "@example.invalid");
        colaborador.setContrasena("hash-de-prueba-no-real");
        colaborador.setRol(rol);
        colaborador.setTipoColaborador(rol);
        em.persist(colaborador);
        return colaborador;
    }

    private Administrador persistirAdministrador(String suffix) {
        Administrador admin = new Administrador();
        admin.setNombres("Admin");
        admin.setApellidos("M13");
        admin.setIdentificacion("A" + suffix);
        admin.setCorreo("m13-admin-" + suffix + "@example.invalid");
        admin.setContrasena("hash-de-prueba-no-real");
        admin.setRol(RolUsuario.ADMINISTRADOR);
        admin.setNivelAcceso(1);
        em.persist(admin);
        em.flush();
        return admin;
    }
}
