package com.congreso.controlador;

import com.congreso.dto.EstadoParticipacion.Paso;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.modelo.enums.RolUsuario;
import com.congreso.servicio.PagoServicio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Ejecutar con -Dtest=EstadoParticipacionPostgresIT; datos ficticios con rollback. */
@SpringBootTest
@ActiveProfiles({"dev", "local"})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false", "spring.sql.init.mode=never"
})
@Transactional
class EstadoParticipacionPostgresIT {
    @Autowired private EntityManager em;
    @Autowired private PagoServicio pagos;
    @Autowired private DataSource dataSource;

    @Test
    void guiaLaInscripcionActualSinMezclarPagosAnteriores() throws Exception {
        try (var conexion = dataSource.getConnection()) {
            assertEquals("PostgreSQL", conexion.getMetaData().getDatabaseProductName());
        }
        String clave = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        Asistente asistente = new Asistente();
        asistente.setNombres("Prueba");
        asistente.setApellidos("Navegación");
        asistente.setCorreo("m08-" + clave + "@example.invalid");
        asistente.setIdentificacion(clave);
        asistente.setContrasena("hash-de-prueba-no-real");
        asistente.setRol(RolUsuario.ASISTENTE);
        em.persist(asistente);
        assertEquals(Paso.CREAR_INSCRIPCION, pagos.estadoParticipacion(asistente.getCorreo()).paso());

        Inscripcion anterior = new Inscripcion(asistente, "Externo");
        anterior.confirmar();
        em.persist(anterior);
        Pago pagado = new Pago(asistente, 100, ModalidadPago.COMPLETO, 1);
        pagado.setInscripcion(anterior);
        pagado.setSaldo(0);
        pagado.setEstado(com.congreso.modelo.enums.EstadoPago.PAGADO);
        em.persist(pagado);
        Inscripcion actual = new Inscripcion(asistente, "Estudiante");
        em.persist(actual);
        em.flush();
        em.clear();

        var pendiente = pagos.estadoParticipacion(asistente.getCorreo());
        assertEquals(actual.getCodigo(), pendiente.codigoInscripcion());
        assertEquals("SIN PAGOS", pendiente.pago());
        assertEquals(Paso.SIMULAR_PAGO, pendiente.paso());

        Pago parcial = pagos.simularPago(asistente.getCorreo(), actual.getCodigo(), ModalidadPago.CUOTAS);
        assertEquals("PARCIAL", pagos.estadoParticipacion(asistente.getCorreo()).pago());
        assertEquals(Paso.COMPLETAR_CUOTA, pagos.estadoParticipacion(asistente.getCorreo()).paso());

        pagos.simularSegundaCuota(asistente.getCorreo(), parcial.getCodigoPago());
        var confirmada = pagos.estadoParticipacion(asistente.getCorreo());
        assertEquals("CONFIRMADA", confirmada.inscripcion());
        assertEquals("PAGADO", confirmada.pago());
        assertEquals(Paso.VER_AGENDA, confirmada.paso());
        assertEquals(actual.getCodigo(), confirmada.codigoInscripcion());
    }
}
