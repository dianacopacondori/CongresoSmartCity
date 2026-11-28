package com.congreso.controlador;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.InscripcionServicio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Requiere PostgreSQL y revierte todas las filas de prueba al terminar. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "local"})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never"
})
@Transactional
class InscripcionConfirmacionPostgresIT {
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;
    @Autowired private InscripcionServicio inscripcionServicio;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void soloConfirmaEnPostgresCuandoElPagoQuedaCompleto() throws Exception {
        try (var conexion = dataSource.getConnection()) {
            assertEquals("PostgreSQL", conexion.getMetaData().getDatabaseProductName());
        }

        Asistente asistente = crearAsistente();
        em.persist(asistente);
        em.flush();

        Inscripcion inscripcion = inscripcionServicio.crearInscripcion(asistente, "Estudiante");
        em.flush();
        Long codigo = inscripcion.getCodigo();
        assertEquals(EstadoInscripcion.PENDIENTE, inscripcion.getEstado());
        var resumen = inscripcionServicio.calcularResumenPago(inscripcion);
        assertEquals(EstadoInscripcion.PENDIENTE, resumen.estado());
        assertNull(resumen.estadoPago());
        assertEquals(50.0, resumen.saldo().doubleValue());

        mvc.perform(post("/inscripciones/confirmar/{codigo}", codigo).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        "No se puede confirmar la inscripción: el pago simulado debe estar completo "
                                + "(estado PAGADO y saldo S/ 0.00)."));
        em.flush();
        em.clear();
        assertEquals(EstadoInscripcion.PENDIENTE, em.find(Inscripcion.class, codigo).getEstado());

        Inscripcion pendiente = em.find(Inscripcion.class, codigo);
        Asistente titular = pendiente.getAsistente();
        Pago pago = new Pago(titular, pendiente.getTarifa().doubleValue(), ModalidadPago.COMPLETO, 1);
        pago.setInscripcion(pendiente);
        pago.registrarPago(pendiente.getTarifa().doubleValue());
        em.persist(pago);
        em.flush();
        assertEquals(EstadoPago.PAGADO, pago.getEstado());
        assertEquals(0.0, pago.getSaldo());

        mvc.perform(post("/inscripciones/confirmar/{codigo}", codigo).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inscripciones/admin/listar"))
                .andExpect(flash().attribute("mensaje", "Inscripción confirmada"));

        em.flush();
        em.clear();
        Inscripcion confirmada = em.find(Inscripcion.class, codigo);
        assertEquals(EstadoInscripcion.CONFIRMADA, confirmada.getEstado());
        assertTrue(confirmada.isConfirmacion());
    }

    private Asistente crearAsistente() {
        String clave = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        Asistente asistente = new Asistente();
        asistente.setNombres("Prueba");
        asistente.setApellidos("A02");
        asistente.setIdentificacion("a02-" + clave);
        asistente.setCorreo("a02-" + clave + "@example.invalid");
        asistente.setContrasena("hash-de-prueba-no-real");
        asistente.setRol(RolUsuario.ASISTENTE);
        return asistente;
    }
}
