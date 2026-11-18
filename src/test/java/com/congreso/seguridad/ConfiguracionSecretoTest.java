package com.congreso.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Evita que credenciales o ajustes inseguros vuelvan a versionarse.
 */
class ConfiguracionSecretoTest {

    private Properties propiedades() throws IOException {
        Properties properties = new Properties();
        try (InputStream entrada = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(entrada);
        }
        return properties;
    }

    @Test
    void laContrasenaDeBaseDeDatosNoEstaEscritaEnElRepositorio() throws IOException {
        Properties properties = propiedades();

        assertEquals("${DB_PASSWORD}", properties.getProperty("spring.datasource.password"));
        assertFalse(properties.stringPropertyNames().stream()
                .anyMatch(clave -> properties.getProperty(clave).contains("6006")));
    }

    @Test
    void lasCredencialesSmtpSonExternalizadas() throws IOException {
        Properties properties = propiedades();

        assertTrue(properties.getProperty("spring.mail.username").startsWith("${MAIL_USERNAME"));
        assertTrue(properties.getProperty("spring.mail.password").startsWith("${MAIL_PASSWORD"));
        assertFalse(properties.toString().contains("tu-correo@gmail.com"));
        assertFalse(properties.toString().contains("tu-password"));
    }

    @Test
    void noSeMuestraSqlNiSeRegistranDatosPersonales() throws IOException {
        Properties properties = propiedades();

        assertEquals("false", properties.getProperty("spring.jpa.show-sql"));
        assertEquals("false", properties.getProperty("spring.jpa.properties.hibernate.format_sql"));
    }

    @Test
    void laCookieDeSesionEstaEndurecida() throws IOException {
        Properties properties = propiedades();

        assertEquals("true", properties.getProperty("server.servlet.session.cookie.http-only"));
        assertEquals("lax", properties.getProperty("server.servlet.session.cookie.same-site"));
    }
}
