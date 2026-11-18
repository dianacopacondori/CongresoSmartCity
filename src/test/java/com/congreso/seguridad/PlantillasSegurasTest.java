package com.congreso.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las plantillas deben poder servirse con una CSP que no permita 'unsafe-inline'
 * ni recursos externos. Esto evita regresiones del hallazgo 90003 de ZAP.
 */
class PlantillasSegurasTest {

    private static final Pattern SCRIPT_INLINE = Pattern.compile("<script\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern MANEJADOR_INLINE =
            Pattern.compile("\\son(click|change|submit|load|input|error|mouseover)\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern RECURSO_EXTERNO = Pattern.compile(
            "https?://(fonts\\.googleapis\\.com|fonts\\.gstatic\\.com|cdn\\.jsdelivr\\.net|cdnjs\\.cloudflare\\.com|unpkg\\.com)",
            Pattern.CASE_INSENSITIVE);

    @Test
    void ningunaPlantillaIncluyeScriptsOManejadoresInline() throws IOException {
        Resource[] plantillas = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:templates/**/*.html");
        assertTrue(plantillas.length > 0, "No se encontraron plantillas para analizar");

        for (Resource plantilla : plantillas) {
            String contenido = new String(plantilla.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(SCRIPT_INLINE.matcher(contenido).find(),
                    "Script inline en " + plantilla.getFilename());
            assertFalse(MANEJADOR_INLINE.matcher(contenido).find(),
                    "Manejador de evento inline en " + plantilla.getFilename());
        }
    }

    @Test
    void ningunaPlantillaCargaRecursosExternos() throws IOException {
        Resource[] plantillas = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:templates/**/*.html");

        for (Resource plantilla : plantillas) {
            String contenido = new String(plantilla.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(RECURSO_EXTERNO.matcher(contenido).find(),
                    "Recurso externo en " + plantilla.getFilename());
        }
    }

    @Test
    void losScriptsLocalesQueSustituyenAlCodigoInlineEstanPresentes() {
        assertTrue(new ClassPathResource("static/js/admin-form.js").exists());
        assertTrue(new ClassPathResource("static/js/admin-navbar.js").exists());
        assertTrue(new ClassPathResource("static/js/confirm-submit.js").exists());
    }
}
