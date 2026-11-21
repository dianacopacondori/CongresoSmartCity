package com.congreso.servicio;

import com.congreso.modelo.Asistente;
import com.congreso.modelo.Colaborador;
import com.congreso.modelo.Inscripcion;
import com.congreso.modelo.Pago;
import com.congreso.modelo.Ponencia;
import com.congreso.modelo.Sala;
import com.congreso.modelo.enums.EstadoPago;
import com.congreso.modelo.enums.ModalidadPago;
import com.congreso.modelo.enums.RolUsuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ejecutar explícitamente con -Dtest=UltimoCupoPostgresIT; borra solo sus filas ficticias. */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false", "spring.sql.init.mode=never"
})
class UltimoCupoPostgresIT {
    @Autowired private InscripcionServicio servicio;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transacciones;

    private record Datos(Long[] asistentes, String[] correos, Long[] inscripciones,
                         Long actividad, Long sala, Long ponente) {}

    @Test
    void dosAsistentesCompitenPorUltimoCupoSinSobreventa() throws Exception {
        Datos datos = transacciones.execute(estado -> crearDatos());
        try {
            CountDownLatch inicio = new CountDownLatch(1);
            var ejecutor = Executors.newFixedThreadPool(2);
            try {
                var primera = ejecutor.submit(() -> intentar(inicio, datos.correos()[0], datos.actividad()));
                var segunda = ejecutor.submit(() -> intentar(inicio, datos.correos()[1], datos.actividad()));
                inicio.countDown();
                boolean r1 = primera.get(20, TimeUnit.SECONDS);
                boolean r2 = segunda.get(20, TimeUnit.SECONDS);
                assertTrue(r1 ^ r2, "Exactamente una inscripción debe obtener el último cupo");
            } finally {
                ejecutor.shutdownNow();
            }
            assertEquals(1, jdbc.queryForObject("SELECT inscritos FROM actividades WHERE codigo = ?",
                    Integer.class, datos.actividad()));
            assertEquals(1L, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM detalle_inscripcion WHERE codigo_actividad = ?", Long.class, datos.actividad()));
        } finally {
            transacciones.executeWithoutResult(estado -> limpiarDatos(datos));
        }
    }

    @Test
    void dosEnviosSimultaneosDelMismoAsistenteNoDuplicanLaActividad() throws Exception {
        Datos datos = transacciones.execute(estado -> crearDatos());
        try {
            CountDownLatch inicio = new CountDownLatch(1);
            var ejecutor = Executors.newFixedThreadPool(2);
            try {
                var primera = ejecutor.submit(() -> intentar(inicio, datos.correos()[0], datos.actividad()));
                var segunda = ejecutor.submit(() -> intentar(inicio, datos.correos()[0], datos.actividad()));
                inicio.countDown();
                assertTrue(primera.get(20, TimeUnit.SECONDS) ^ segunda.get(20, TimeUnit.SECONDS),
                        "Un reenvío simultáneo no debe duplicar la selección");
            } finally {
                ejecutor.shutdownNow();
            }
            assertEquals(1, jdbc.queryForObject("SELECT inscritos FROM actividades WHERE codigo = ?",
                    Integer.class, datos.actividad()));
            assertEquals(1L, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM detalle_inscripcion WHERE codigo_actividad = ?", Long.class, datos.actividad()));
        } finally {
            transacciones.executeWithoutResult(estado -> limpiarDatos(datos));
        }
    }

    private boolean intentar(CountDownLatch inicio, String correo, Long actividad) throws InterruptedException {
        inicio.await();
        try {
            return servicio.inscribirActividadPropia(correo, actividad);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private Datos crearDatos() {
        String clave = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        Asistente primero = asistente(clave + "a");
        Asistente segundo = asistente(clave + "b");
        Colaborador ponente = new Colaborador();
        ponente.setNombres("Ponente");
        ponente.setApellidos("Prueba");
        ponente.setIdentificacion("p" + clave);
        ponente.setCorreo("cupo-p" + clave + "@example.invalid");
        ponente.setContrasena("hash-de-prueba-no-real");
        ponente.setRol(RolUsuario.PONENTE);
        ponente.setTipoColaborador(RolUsuario.PONENTE);
        em.persist(ponente);
        Sala sala = new Sala("Sala cupo " + clave, 1, "Prueba");
        em.persist(sala);
        Ponencia ponencia = new Ponencia();
        ponencia.setTitulo("Último cupo " + clave);
        ponencia.setSala(sala);
        ponencia.setPonente(ponente);
        ponencia.setFecha(LocalDate.now().plusDays(7));
        ponencia.setHorarioInicio(LocalTime.of(10, 0));
        ponencia.setHorarioFin(LocalTime.of(11, 0));
        ponencia.setCupo(1);
        em.persist(ponencia);

        Inscripcion i1 = inscripcionPagada(primero);
        Inscripcion i2 = inscripcionPagada(segundo);
        em.flush();
        return new Datos(new Long[]{primero.getCodigo(), segundo.getCodigo()},
                new String[]{primero.getCorreo(), segundo.getCorreo()},
                new Long[]{i1.getCodigo(), i2.getCodigo()}, ponencia.getCodigo(), sala.getCodigo(), ponente.getCodigo());
    }

    private Asistente asistente(String clave) {
        Asistente asistente = new Asistente();
        asistente.setNombres("Participante");
        asistente.setApellidos("Prueba");
        asistente.setIdentificacion(clave);
        asistente.setCorreo("cupo-" + clave + "@example.invalid");
        asistente.setContrasena("hash-de-prueba-no-real");
        asistente.setRol(RolUsuario.ASISTENTE);
        em.persist(asistente);
        return asistente;
    }

    private Inscripcion inscripcionPagada(Asistente asistente) {
        Inscripcion inscripcion = new Inscripcion(asistente, "Estudiante");
        inscripcion.confirmar();
        em.persist(inscripcion);
        Pago pago = new Pago(asistente, 50, ModalidadPago.COMPLETO, 1);
        pago.setInscripcion(inscripcion);
        pago.setEstado(EstadoPago.PAGADO);
        pago.setSaldo(0);
        em.persist(pago);
        return inscripcion;
    }

    private void limpiarDatos(Datos datos) {
        jdbc.update("DELETE FROM detalle_inscripcion WHERE codigo_inscripcion IN (?, ?)", (Object[]) datos.inscripciones());
        jdbc.update("DELETE FROM pagos WHERE codigo_inscripcion IN (?, ?)", (Object[]) datos.inscripciones());
        jdbc.update("DELETE FROM inscripciones WHERE codigo IN (?, ?)", (Object[]) datos.inscripciones());
        jdbc.update("DELETE FROM ponencias WHERE codigo_actividad = ?", datos.actividad());
        jdbc.update("DELETE FROM actividades WHERE codigo = ?", datos.actividad());
        jdbc.update("DELETE FROM salas WHERE codigo = ?", datos.sala());
        jdbc.update("DELETE FROM colaboradores WHERE codigo_usuario = ?", datos.ponente());
        jdbc.update("DELETE FROM asistentes WHERE codigo_usuario IN (?, ?)", (Object[]) datos.asistentes());
        jdbc.update("DELETE FROM usuarios WHERE codigo IN (?, ?, ?)",
                datos.asistentes()[0], datos.asistentes()[1], datos.ponente());
    }
}
