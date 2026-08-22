package com.nexafarma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean de cifrado de contrasenas, separado de la configuracion completa de
 * Spring Security (filter chain, roles, etc.) para que UsuarioService pueda
 * usarlo desde ya sin acoplarse a esa configuracion, que se define en el
 * paquete `security` mas adelante.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
