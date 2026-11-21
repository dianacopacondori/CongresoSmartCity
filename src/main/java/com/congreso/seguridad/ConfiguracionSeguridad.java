package com.congreso.seguridad;

import com.congreso.dao.UsuarioDAO;
import com.congreso.modelo.Administrador;
import com.congreso.modelo.Asistente;
import com.congreso.modelo.Usuario;
import com.congreso.modelo.enums.RolUsuario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class ConfiguracionSeguridad {

    @Bean
    public UserDetailsService userDetailsService(UsuarioDAO usuarioDAO) {
        return correo -> {
            Usuario usuario = usuarioDAO.findByCorreo(correo)
                    .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
            validarRol(usuario);
            return User.withUsername(usuario.getCorreo())
                    .password(usuario.getContrasena())
                    .roles(usuario.getRol().name())
                    .disabled(!usuario.estaActivo())
                    .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioDAO usuarioDAO) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/ponentes", "/contacto", "/actividades",
                        "/usuarios/login", "/usuarios/registro", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/**", "/asistentes/admin/**", "/pagos/admin/**",
                        "/inscripciones/admin/**", "/inscripciones/confirmar/**",
                        "/asistencias/admin/**", "/asistencias/registrar",
                        "/reportes/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/dashboard-asistente").hasRole("ASISTENTE")
                .requestMatchers("/actividades/inscribir/**").hasRole("ASISTENTE")
                .requestMatchers("/inscripciones/agregar-actividad").hasRole("ASISTENTE")
                .requestMatchers("/asistentes/mi-perfil").hasRole("ASISTENTE")
                .requestMatchers("/pagos/nuevo", "/pagos/confirmacion", "/pagos/mis-pagos", "/pagos/abonar/**")
                    .hasRole("ASISTENTE")
                .anyRequest().authenticated()
        );
        http.formLogin(form -> form
                .loginPage("/usuarios/login")
                .loginProcessingUrl("/usuarios/login")
                .usernameParameter("correo")
                .passwordParameter("contrasena")
                .failureUrl("/usuarios/login?error")
                .successHandler((request, response, authentication) -> {
                    Usuario usuario = usuarioDAO.findByCorreo(authentication.getName())
                            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
                    validarRol(usuario);
                    request.getSession().setAttribute("usuario", usuario);
                    request.getSession().setAttribute("rol", usuario.getRol());
                    String destino = usuario.getRol() == RolUsuario.ADMINISTRADOR
                            ? "/admin/dashboard" : usuario.getRol() == RolUsuario.ASISTENTE
                            ? "/dashboard-asistente" : "/";
                    response.sendRedirect(request.getContextPath() + destino);
                })
                .permitAll()
        );
        http.logout(logout -> logout
                .logoutUrl("/usuarios/logout")
                .logoutSuccessUrl("/usuarios/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        );
        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self'; " +
                        "style-src 'self'; " +
                        "font-src 'self' data:; " +
                        "img-src 'self' data:; " +
                        "connect-src 'self'; " +
                        "object-src 'none'; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'self'; " +
                        "form-action 'self'"))
                .referrerPolicy(referrer -> referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000))
                .addHeaderWriter(new StaticHeadersWriter(
                        "Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
        );
        return http.build();
    }

    private static void validarRol(Usuario usuario) {
        if ((usuario instanceof Administrador && usuario.getRol() != RolUsuario.ADMINISTRADOR)
                || (usuario instanceof Asistente && usuario.getRol() != RolUsuario.ASISTENTE)
                || (!(usuario instanceof Administrador) && usuario.getRol() == RolUsuario.ADMINISTRADOR)) {
            throw new UsernameNotFoundException("Credenciales inválidas");
        }
    }
}
