package com.congreso.servicio;

import com.congreso.modelo.Actividad;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Asistencia;
import com.congreso.modelo.Fotocheck;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Sala;
import com.congreso.modelo.enums.EstadoActividad;
import com.congreso.modelo.enums.EstadoInscripcion;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.modelo.enums.RolUsuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ejecutar explícitamente con -Dtest=AsistenciaRegistroPostgresIT; datos ficticios con rollback. */
@SpringBootTest
@Transactional
class AsistenciaRegistroPostgresIT {

    @Autowired private AsistenciaServicio servicio;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void registraAsistenciaVerificadaConInscripcionPagadaYFotocheck() throws Exception {
        try (var conexion = dataSource.getConnection()) {
            assertTrue(conexion.getMetaData().getDatabaseProductName().contains("PostgreSQL"));
        }

        String clave = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        Asistente asistente = new Asistente();
        asistente.setNombres("Prueba");
        asistente.setApellidos("Asistencia");
        asistente.setCorreo("asistencia-" + clave + "@example.invalid");
        asistente.setIdentificacion(clave);
        asistente.setContrasena("hash-de-prueba-no-real");
        asistente.setRol(RolUsuario.ASISTENTE);
        em.persist(asistente);

        Administrador admin = new Administrador();
        admin.setNombres("Admin");
        admin.setApellidos("Prueba");
        admin.setCorreo("admin-" + clave + "@example.invalid");
        admin.setIdentificacion("A" + clave);
        admin.setContrasena("hash-de-prueba-no-real");
        admin.setRol(RolUsuario.ADMINISTRADOR);
        admin.setNivelAcceso(1);
        em.persist(admin);

        Colaborador ponente = new Colaborador();
        ponente.setNombres("Ponente");
        ponente.setApellidos("Prueba");
        ponente.setCorreo("ponente-" + clave + "@example.invalid");
        ponente.setIdentificacion("P" + clave);
        ponente.setContrasena("hash-de-prueba-no-real");
        ponente.setRol(RolUsuario.PONENTE);
        ponente.setTipoColaborador(RolUsuario.PONENTE);
        em.persist(ponente);

        Sala sala = new Sala("Sala " + clave, 50, "Local");
        em.persist(sala);
        Ponencia actividad = new Ponencia();
        actividad.setPonente(ponente);
        actividad.setTitulo("Ponencia " + clave);
        actividad.setFecha(LocalDate.now());
        actividad.setHorarioInicio(LocalTime.of(9, 0));
        actividad.setHorarioFin(LocalTime.of(10, 0));
        actividad.setCupo(10);
        actividad.setInscritos(0);
        actividad.setEstado(EstadoActividad.ACTIVA);
        actividad.setSala(sala);
        em.persist(actividad);

        Inscripcion inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.setEstado(EstadoInscripcion.CONFIRMADA);
        inscripcion.setConfirmacion(true);
        inscripcion.agregarActividad(actividad);
        em.persist(inscripcion);

        Pago pago = new Pago(asistente, 50, ModalidadPago.COMPLETO, 1);
        pago.setInscripcion(inscripcion);
        pago.setEstado(EstadoPago.PAGADO);
        pago.setSaldo(0);
        em.persist(pago);

        Fotocheck fotocheck = new Fotocheck(asistente);
        em.persist(fotocheck);
        em.flush();

        Asistencia registrada = servicio.registrarAsistencia(
                asistente.getCodigo(), actividad.getCodigo(), "CODIGO",
                fotocheck.getCodigo(), admin.getCorreo());
        em.flush();

        assertEquals(actividad.getFecha(), registrada.getFecha());
        assertEquals("CODIGO", registrada.getMecanismoValidacion());
        assertEquals(admin.getCodigo(), registrada.getRegistradoPor().getCodigo());

        Integer restricciones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = ?", Integer.class,
                "uk_asistencia_asistente_actividad");
        assertEquals(1, restricciones);
        String detalle = jdbcTemplate.queryForObject(
                "SELECT detalle FROM migraciones_aplicadas WHERE id = ?", String.class,
                "A07-uk-asistencia-par");
        assertTrue(detalle.startsWith("aplicada") || detalle.startsWith("omitida: la restriccion ya existia"));

        servicio.corregirAsistencia(registrada.getCodigo(), "Fotocheck verificado en recepcion", admin.getCorreo());
        em.flush();
        assertEquals("Fotocheck verificado en recepcion", registrada.getJustificacionCorreccion());
        assertEquals(admin.getCodigo(), registrada.getCorregidoPor().getCodigo());
        assertEquals(java.time.LocalDate.now(), registrada.getFechaCorreccion().toLocalDate());
    }
}
