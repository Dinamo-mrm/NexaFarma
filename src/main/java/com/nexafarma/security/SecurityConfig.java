package com.nexafarma.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORAL — Desarrollador 1 aún no ha implementado la autenticación real
 * (login + roles Administrador/Farmacéutico/Vendedor/Auxiliar).
 *
 * <p>Con {@code spring-boot-starter-security} en el classpath y sin ningún
 * {@link SecurityFilterChain} propio, Spring Boot activa su configuración
 * por defecto: TODOS los endpoints (incluida la interfaz web y la API REST)
 * quedan detrás de un login con una contraseña generada al azar en cada
 * arranque. Esta clase desactiva esa protección por defecto para poder
 * trabajar directamente sobre la interfaz mientras se construye el resto
 * del sistema.</p>
 *
 * <p><b>Reemplazar</b> por la configuración real de Dev1 (matriz de roles
 * por endpoint) antes de cualquier despliegue fuera del entorno de
 * desarrollo — con esta configuración, cualquiera con acceso a la URL
 * puede leer y modificar todos los datos, sin autenticarse.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable()) // la API REST aun no maneja tokens CSRF/sesion
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }
}
