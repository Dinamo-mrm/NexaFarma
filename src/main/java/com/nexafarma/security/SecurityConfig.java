package com.nexafarma.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexafarma.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/** Configura la autenticacion con usuarios de base de datos y permisos por rol. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/login", "/error").permitAll()
                        .requestMatchers("/api/usuarios/**", "/api/empleados/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/compras/**", "/api/formulas-medicas/**", "/api/devoluciones/**")
                        .hasAnyRole("ADMINISTRADOR", "FARMACEUTICO")
                        .requestMatchers("/api/ventas/**", "/api/clientes/**", "/api/domicilios/**")
                        .hasAnyRole("ADMINISTRADOR", "FARMACEUTICO", "VENDEDOR", "AUXILIAR")
                        .requestMatchers("/api/alertas/**", "/api/inventario/**", "/api/lotes/**",
                                "/api/medicamentos/**", "/api/movimientos-inventario/**",
                                "/api/categorias/**", "/api/proveedores/**")
                        .hasAnyRole("ADMINISTRADOR", "FARMACEUTICO", "VENDEDOR", "AUXILIAR")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").permitAll())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor((request, response, exception) ->
                                        escribirError(response, HttpStatus.UNAUTHORIZED,
                                                "Debes autenticarte para acceder a este recurso.", objectMapper),
                                new AntPathRequestMatcher("/api/**"))
                        .defaultAccessDeniedHandlerFor((request, response, exception) ->
                                        escribirError(response, HttpStatus.FORBIDDEN,
                                                "No tienes permisos para realizar esta accion.", objectMapper),
                                new AntPathRequestMatcher("/api/**")));
        return http.build();
    }

    private static void escribirError(HttpServletResponse response, HttpStatus estado, String mensaje,
                                      ObjectMapper objectMapper) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiError(
                LocalDateTime.now(), estado.value(), estado.getReasonPhrase(), mensaje, null, Map.of()));
    }
}
