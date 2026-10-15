package com.congreso.seguridad;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Componente para la encriptación y verificación de contraseñas.
 * Utiliza BCrypt para encriptación segura.
 * @author cvdiana
 * @version 1.0.0
 */
@Component
public class EncriptadorPassword {

    private final PasswordEncoder passwordEncoder;

    public EncriptadorPassword() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Encripta una contraseña en texto plano.
     *
     * @param contrasenaPlana la contraseña en texto plano
     * @return la contraseña encriptada
     */
    public String encriptar(String contrasenaPlana) {
        return passwordEncoder.encode(contrasenaPlana);
    }

    public boolean verificar(String contrasenaPlana, String contrasenaEncriptada) {
        return passwordEncoder.matches(contrasenaPlana, contrasenaEncriptada);
    }
}
