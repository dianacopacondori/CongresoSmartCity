package com.congreso.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        assertEquals("${DB_USERNAME}", properties.getProperty("spring.datasource.username"));
        assertEquals("${DB_PASSWORD}", properties.getProperty("spring.datasource.password"));
        assertFalse(properties.stringPropertyNames().stream()
                .anyMatch(clave -> properties.getProperty(clave).contains("6006")));
    }

    @Test
    void lasCredencialesLocalesSeImportanSinNecesidadDePerfil() throws IOException {
        Properties properties = propiedades();
        assertTrue(properties.getProperty("spring.config.import").contains("application-local.properties"));

        ClassPathResource local = new ClassPathResource("application-local.properties");
        if (local.exists()) {
            Properties locales = new Properties();
            try (InputStream entrada = local.getInputStream()) {
                locales.load(entrada);
            }
            assertTrue(locales.getProperty("spring.datasource.username").contains("DB_USERNAME"));
            assertTrue(locales.getProperty("spring.datasource.password").contains("DB_PASSWORD"));
        }
    }

    @Test
    void noHayPerfilesActivosPorDefectoNiActualizacionAutomaticaDelEsquema() throws IOException {
        Properties properties = propiedades();

        assertNull(properties.getProperty("spring.profiles.active"));
        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
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
