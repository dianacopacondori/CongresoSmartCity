package com.congreso.controlador;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.RolUsuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ejecutar explícitamente con -Dtest=PagoSimuladoPostgresIT. Datos ficticios y rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false", "spring.sql.init.mode=never"
})
@Transactional
class PagoSimuladoPostgresIT {
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;

    @Test
    void confirmaExternoSoloConPagoCompletoYNoAceptaImportesDelCliente() throws Exception {
        try (var conexion = dataSource.getConnection()) {
            assertEquals("PostgreSQL", conexion.getMetaData().getDatabaseProductName());
        }
        Asistente asistente = asistente();
        Inscripcion inscripcion = inscripcion(asistente, "Externo");
        Long id = inscripcion.getCodigo();
        em.flush();
        em.clear();

        mvc.perform(post("/pagos/nuevo").with(user(asistente.getCorreo()).roles("ASISTENTE")).with(csrf())
                        .param("codigoInscripcion", id.toString()).param("modalidad", "COMPLETO")
                        .param("importe", "0").param("saldo", "0").param("estado", "PAGADO")
                        .param("asistente.codigo", "9999"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pagos/confirmacion"))
                .andExpect(flash().attributeExists("mensaje"));

        em.flush();
        em.clear();
        var pagos = em.createQuery("SELECT p FROM Pago p WHERE p.inscripcion.codigo = :id", Pago.class)
                .setParameter("id", id).getResultList();
        assertEquals(1, pagos.size());
        assertEquals(100.0, pagos.get(0).getImporte());
        assertEquals(0.0, pagos.get(0).getSaldo());
        assertEquals(EstadoPago.PAGADO, pagos.get(0).getEstado());
        assertEquals(EstadoInscripcion.CONFIRMADA, em.find(Inscripcion.class, id).getEstado());
    }

    @Test
    void dosCuotasEstudiantilesConfirmanSoloTrasLaSegundaYNoSeDuplican() throws Exception {
        Asistente asistente = asistente();
        Inscripcion inscripcion = inscripcion(asistente, "Estudiante");
        Long id = inscripcion.getCodigo();
        em.flush();
        em.clear();

        mvc.perform(post("/pagos/nuevo").with(user(asistente.getCorreo()).roles("ASISTENTE")).with(csrf())
                        .param("codigoInscripcion", id.toString()).param("modalidad", "CUOTAS"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pagos/confirmacion"));
        em.flush();
        em.clear();
        Pago parcial = em.createQuery("SELECT p FROM Pago p WHERE p.inscripcion.codigo = :id", Pago.class)
                .setParameter("id", id).getSingleResult();
        Long pagoId = parcial.getCodigoPago();
        assertEquals(50.0, parcial.getImporte());
        assertEquals(25.0, parcial.getSaldo());
        assertEquals(EstadoPago.PARCIAL, parcial.getEstado());
        assertFalse(em.find(Inscripcion.class, id).isConfirmacion());

        mvc.perform(post("/pagos/nuevo").with(user(asistente.getCorreo()).roles("ASISTENTE")).with(csrf())
                        .param("codigoInscripcion", id.toString()).param("modalidad", "COMPLETO"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pagos/nuevo"))
                .andExpect(flash().attributeExists("error"));

        mvc.perform(post("/pagos/abonar/{id}", pagoId).with(user(asistente.getCorreo()).roles("ASISTENTE"))
                        .with(csrf()).param("monto", "999"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pagos/confirmacion"));
        em.flush();
        em.clear();
        Pago completado = em.find(Pago.class, pagoId);
        assertEquals(0.0, completado.getSaldo());
        assertEquals(EstadoPago.PAGADO, completado.getEstado());
        assertTrue(em.find(Inscripcion.class, id).isConfirmacion());

        mvc.perform(post("/pagos/abonar/{id}", pagoId).with(user(asistente.getCorreo()).roles("ASISTENTE"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pagos/mis-pagos"))
                .andExpect(flash().attributeExists("error"));
        assertEquals(1L, em.createQuery("SELECT COUNT(p) FROM Pago p WHERE p.inscripcion.codigo = :id", Long.class)
                .setParameter("id", id).getSingleResult());
    }

    @Test
    void rechazaPagosAjenosCuotasExternasYAccesoSinCsrf() throws Exception {
        Asistente propietario = asistente();
        Asistente intruso = asistente();
        Inscripcion inscripcion = inscripcion(propietario, "Externo");
        Long id = inscripcion.getCodigo();
        em.flush();
        em.clear();

        mvc.perform(post("/pagos/nuevo").with(user(intruso.getCorreo()).roles("ASISTENTE")).with(csrf())
                        .param("codigoInscripcion", id.toString()).param("modalidad", "COMPLETO"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/pagos/nuevo").with(user(propietario.getCorreo()).roles("ASISTENTE")).with(csrf())
                        .param("codigoInscripcion", id.toString()).param("modalidad", "CUOTAS"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/pagos/nuevo"))
                .andExpect(flash().attributeExists("error"));
        mvc.perform(post("/pagos/nuevo").with(user(propietario.getCorreo()).roles("ASISTENTE"))
                        .param("codigoInscripcion", id.toString()).param("modalidad", "COMPLETO"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/pagos/nuevo").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("codigoInscripcion", id.toString()).param("modalidad", "COMPLETO"))
                .andExpect(status().isForbidden());
        assertEquals(0L, em.createQuery("SELECT COUNT(p) FROM Pago p WHERE p.inscripcion.codigo = :id", Long.class)
                .setParameter("id", id).getSingleResult());
        assertEquals(EstadoInscripcion.PENDIENTE, em.find(Inscripcion.class, id).getEstado());
        mvc.perform(get("/pagos/mis-pagos").with(user(intruso.getCorreo()).roles("ASISTENTE")))
                .andExpect(status().isOk());
    }

    private Asistente asistente() {
        String clave = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        Asistente asistente = new Asistente();
        asistente.setNombres("Prueba");
        asistente.setApellidos("Pago");
        asistente.setCorreo("pago-" + clave + "@example.invalid");
        asistente.setIdentificacion(clave);
        asistente.setContrasena("hash-de-prueba-no-real");
        asistente.setRol(RolUsuario.ASISTENTE);
        em.persist(asistente);
        return asistente;
    }

    private Inscripcion inscripcion(Asistente asistente, String tipo) {
        Inscripcion inscripcion = new Inscripcion(asistente, tipo);
        em.persist(inscripcion);
        return inscripcion;
    }
}
