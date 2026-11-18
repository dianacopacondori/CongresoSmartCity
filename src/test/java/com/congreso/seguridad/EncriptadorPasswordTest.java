package com.congreso.seguridad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que las contrasenas nunca se almacenen en texto plano.
 */
class EncriptadorPasswordTest {

    private final EncriptadorPassword encriptador = new EncriptadorPassword();

    @Test
    void elHashNoContieneLaContrasenaEnPlano() {
        String hash = encriptador.encriptar("ContrasenaSegura123!");

        assertFalse(hash.contains("ContrasenaSegura123!"));
        assertTrue(hash.startsWith("$2"));
    }

    @Test
    void verificaLaContrasenaCorrecta() {
        String hash = encriptador.encriptar("ContrasenaSegura123!");

        assertTrue(encriptador.verificar("ContrasenaSegura123!", hash));
    }

    @Test
    void rechazaUnaContrasenaIncorrecta() {
        String hash = encriptador.encriptar("ContrasenaSegura123!");

        assertFalse(encriptador.verificar("ContrasenaErronea", hash));
    }

    @Test
    void usaSaltDiferenteParaCadaContrasena() {
        String primerHash = encriptador.encriptar("MismaContrasena");
        String segundoHash = encriptador.encriptar("MismaContrasena");

        assertNotEquals(primerHash, segundoHash);
    }
}
