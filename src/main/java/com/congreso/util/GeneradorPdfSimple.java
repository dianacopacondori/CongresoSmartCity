package com.congreso.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generador de PDF válido sin dependencias externas: una página por bloque de líneas,
 * fuente Helvetica estándar y codificación Latin-1.
 */
public final class GeneradorPdfSimple {

    private GeneradorPdfSimple() {
    }

    public static byte[] generar(List<String> lineas, int lineasPorPagina) {
        List<List<String>> paginas = new ArrayList<>();
        if (lineas.isEmpty()) {
            paginas.add(List.of("Sin resultados"));
        } else {
            for (int i = 0; i < lineas.size(); i += lineasPorPagina) {
                paginas.add(lineas.subList(i, Math.min(i + lineasPorPagina, lineas.size())));
            }
        }

        int numeroPaginas = paginas.size();
        int primerContenido = 4 + numeroPaginas;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        List<Integer> desplazamientos = new ArrayList<>();

        try {
            salida.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
            StringBuilder hijos = new StringBuilder();
            for (int i = 0; i < numeroPaginas; i++) {
                if (i > 0) hijos.append(' ');
                hijos.append(4 + i).append(" 0 R");
            }
            escribirObjeto(salida, desplazamientos, 1, "<< /Type /Catalog /Pages 2 0 R >>", null);
            escribirObjeto(salida, desplazamientos, 2,
                    "<< /Type /Pages /Kids [" + hijos + "] /Count " + numeroPaginas + " >>", null);
            escribirObjeto(salida, desplazamientos, 3,
                    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>", null);

            for (int i = 0; i < numeroPaginas; i++) {
                escribirObjeto(salida, desplazamientos, 4 + i,
                        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                                + "/Resources << /Font << /F1 3 0 R >> >> /Contents " + (primerContenido + i) + " 0 R >>",
                        null);
            }

            for (int i = 0; i < paginas.size(); i++) {
                StringBuilder contenido = new StringBuilder("BT /F1 10 Tf 50 750 Td 12 TL\n");
                for (String linea : paginas.get(i)) {
                    contenido.append('(').append(escapar(linea)).append(") Tj T*\n");
                }
                contenido.append("ET\n");
                byte[] bytesContenido = contenido.toString().getBytes(StandardCharsets.ISO_8859_1);
                escribirObjeto(salida, desplazamientos, primerContenido + i,
                        "<< /Length " + bytesContenido.length + " >>\nstream", bytesContenido);
            }

            long inicioXref = salida.size();
            int totalObjetos = 3 + 2 * numeroPaginas;
            salida.write(("xref\n0 " + (totalObjetos + 1) + "\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
            salida.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
            for (int desplazamiento : desplazamientos) {
                salida.write((String.format(Locale.US, "%010d 00000 n \n", desplazamiento))
                        .getBytes(StandardCharsets.ISO_8859_1));
            }
            salida.write(("trailer\n<< /Size " + (totalObjetos + 1) + " /Root 1 0 R >>\n"
                    + "startxref\n" + inicioXref + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1));
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    private static void escribirObjeto(ByteArrayOutputStream salida, List<Integer> desplazamientos,
                                       int numero, String cabecera, byte[] contenido) throws IOException {
        desplazamientos.add(salida.size());
        salida.write((numero + " 0 obj\n" + cabecera + "\n").getBytes(StandardCharsets.ISO_8859_1));
        if (contenido != null) {
            salida.write(contenido);
            salida.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        } else {
            salida.write("endobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private static String escapar(String texto) {
        StringBuilder escape = new StringBuilder();
        String valor = texto == null ? "" : texto;
        for (int i = 0; i < valor.length(); i++) {
            char caracter = valor.charAt(i);
            if (caracter == '\\' || caracter == '(' || caracter == ')') {
                escape.append('\\').append(caracter);
            } else if (caracter < 32 || caracter > 255) {
                escape.append('?');
            } else {
                escape.append(caracter);
            }
        }
        return escape.toString();
    }
}
