package com.congreso.controlador;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Sala;
import com.congreso.modelo.enums.RolUsuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ejecutar explícitamente con -Dtest=InscripcionTitularidadPostgresIT. Datos con rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false", "spring.sql.init.mode=never"
})
@Transactional
class InscripcionTitularidadPostgresIT {
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;

    private Asistente titular;
    private Asistente otra;
    private Inscripcion propia;
    private Inscripcion ajena;
    private Ponencia ponencia;

    @BeforeEach
    void datosFicticios() {
        titular = asistente();
        otra = asistente();
        propia = new Inscripcion(titular, "Estudiante");
        propia.confirmar();
        em.persist(propia);
        ajena = new Inscripcion(otra, "Externo");
        ajena.confirmar();
        em.persist(ajena);

        Colaborador ponente = new Colaborador();
        String clave = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        ponente.setNombres("Ponente");
        ponente.setApellidos("Ficticio");
        ponente.setCorreo("c03-ponente-" + clave + "@example.invalid");
        ponente.setIdentificacion("p" + clave);
        ponente.setContrasena("hash-de-prueba-no-real");
        ponente.setRol(RolUsuario.PONENTE);
        ponente.setTipoColaborador(RolUsuario.PONENTE);
        em.persist(ponente);

        Sala sala = new Sala("Sala prueba " + clave, 20, "Prueba");
        em.persist(sala);
        ponencia = new Ponencia();
        ponencia.setTitulo("Prueba de titularidad");
        ponencia.setFecha(LocalDate.now().plusDays(10));
        ponencia.setHorarioInicio(LocalTime.of(9, 0));
        ponencia.setHorarioFin(LocalTime.of(10, 0));
        ponencia.setCupo(20);
        ponencia.setSala(sala);
        ponencia.setPonente(ponente);
        em.persist(ponencia);
        em.flush();
        em.clear();
    }

    @Test
    void noPuedeAlterarInscripcionAjenaNiConsumirCupo() throws Exception {
        try (var conexion = dataSource.getConnection()) {
            assertEquals("PostgreSQL", conexion.getMetaData().getDatabaseProductName());
        }
        mvc.perform(post("/inscripciones/agregar-actividad")
                        .with(user(titular.getCorreo()).roles("ASISTENTE")).with(csrf())
                        .param("codigoInscripcion", ajena.getCodigo().toString())
                        .param("codigoActividad", ponencia.getCodigo().toString()))
                .andExpect(status().isForbidden());

        em.clear();
        assertEquals(0, em.find(Inscripcion.class, ajena.getCodigo()).getActividades().size());
        assertEquals(0, em.find(Ponencia.class, ponencia.getCodigo()).getInscritos());
    }

    @Test
    void soloLaInscripcionPropiaConfirmadaGuardaLaActividad() throws Exception {
        mvc.perform(post("/inscripciones/agregar-actividad")
                        .with(user(titular.getCorreo()).roles("ASISTENTE")).with(csrf())
                        .param("codigoInscripcion", propia.getCodigo().toString())
                        .param("codigoActividad", ponencia.getCodigo().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inscripciones/mi-agenda"))
                .andExpect(flash().attributeExists("mensaje"));

        em.flush();
        em.clear();
        assertEquals(1, em.find(Inscripcion.class, propia.getCodigo()).getActividades().size());
        assertEquals(0, em.find(Inscripcion.class, ajena.getCodigo()).getActividades().size());
        assertEquals(1, em.find(Ponencia.class, ponencia.getCodigo()).getInscritos());
    }

    private Asistente asistente() {
        String clave = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        Asistente asistente = new Asistente();
        asistente.setNombres("Prueba");
        asistente.setApellidos("Titularidad");
        asistente.setCorreo("c03-" + clave + "@example.invalid");
        asistente.setIdentificacion(clave);
        asistente.setContrasena("hash-de-prueba-no-real");
        asistente.setRol(RolUsuario.ASISTENTE);
        em.persist(asistente);
        return asistente;
    }
}
