package com.nexafarma.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexafarma.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Permisos Colombia:
 * - ADMINISTRADOR / FARMACEUTICO (Regente): catálogo, precios, compras, reportes, alertas, editar clientes.
 * - VENDEDOR / AUXILIAR (Auxiliar de Farmacia): ventas, crear/consultar clientes, domicilios, consulta de stock.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] ADMIN_REGENTE = {"ADMINISTRADOR", "FARMACEUTICO"};
    private static final String[] TODOS_OPERATIVOS = {"ADMINISTRADOR", "FARMACEUTICO", "VENDEDOR", "AUXILIAR"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/login", "/error").permitAll()

                        // Solo administrador de sistema
                        .requestMatchers("/api/usuarios/**", "/api/empleados/**").hasRole("ADMINISTRADOR")

                        // Admin + Regente: compras, devoluciones, fórmulas, reportes, alertas de gestión
                        .requestMatchers("/api/compras/**", "/api/formulas-medicas/**", "/api/devoluciones/**",
                                "/api/reportes/**", "/api/alertas/**", "/api/crm/**",
                                "/api/proveedores/**", "/api/categorias/**",
                                "/api/movimientos-inventario/**")
                        .hasAnyRole(ADMIN_REGENTE)

                        // Medicamentos: lectura todos; escritura (precios) solo Admin/Regente
                        .requestMatchers(HttpMethod.GET, "/api/medicamentos/**")
                        .hasAnyRole(TODOS_OPERATIVOS)
                        .requestMatchers(HttpMethod.POST, "/api/medicamentos/**")
                        .hasAnyRole(ADMIN_REGENTE)
                        .requestMatchers(HttpMethod.PUT, "/api/medicamentos/**")
                        .hasAnyRole(ADMIN_REGENTE)
                        .requestMatchers(HttpMethod.DELETE, "/api/medicamentos/**")
                        .hasAnyRole(ADMIN_REGENTE)
                        .requestMatchers(HttpMethod.PATCH, "/api/medicamentos/**")
                        .hasAnyRole(ADMIN_REGENTE)

                        // Clientes: crear y consultar auxiliares; editar/borrar solo Admin/Regente
                        .requestMatchers(HttpMethod.POST, "/api/clientes/**")
                        .hasAnyRole(TODOS_OPERATIVOS)
                        .requestMatchers(HttpMethod.GET, "/api/clientes/**")
                        .hasAnyRole(TODOS_OPERATIVOS)
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/**")
                        .hasAnyRole(ADMIN_REGENTE)
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**")
                        .hasAnyRole(ADMIN_REGENTE)
                        .requestMatchers(HttpMethod.PATCH, "/api/clientes/**")
                        .hasAnyRole(ADMIN_REGENTE)

                        // Inventario/lotes: consulta operativa; liberar cuarentena Admin/Regente
                        .requestMatchers(HttpMethod.GET, "/api/inventario/**", "/api/lotes/**")
                        .hasAnyRole(TODOS_OPERATIVOS)
                        .requestMatchers(HttpMethod.POST, "/api/lotes/**", "/api/inventario/**")
                        .hasAnyRole(ADMIN_REGENTE)
                        .requestMatchers(HttpMethod.PATCH, "/api/lotes/**")
                        .hasAnyRole(ADMIN_REGENTE)

                        // Ventas y domicilios: todos los roles operativos
                        .requestMatchers("/api/ventas/**", "/api/domicilios/**")
                        .hasAnyRole(TODOS_OPERATIVOS)

                        // Vistas web sensibles
                        .requestMatchers("/reportes", "/reportes/**", "/compras", "/compras/**",
                                "/proveedores", "/proveedores/**", "/empleados", "/empleados/**",
                                "/usuarios", "/usuarios/**")
                        .hasAnyRole(ADMIN_REGENTE)

                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                (request, response, exception) ->
                                        escribirError(response, HttpStatus.UNAUTHORIZED,
                                                "Debes iniciar sesion para acceder a este recurso.", objectMapper),
                                new AntPathRequestMatcher("/api/**"))
                        .defaultAccessDeniedHandlerFor(
                                (request, response, exception) ->
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
