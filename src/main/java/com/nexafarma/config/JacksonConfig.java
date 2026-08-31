package com.nexafarma.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los controladores REST de este proyecto devuelven entidades JPA
 * directamente (no DTOs). Con {@code spring.jpa.open-in-view=false}, la
 * sesión de Hibernate se cierra apenas termina la transacción del service,
 * antes de que Jackson serialice la respuesta. Sin este módulo, cualquier
 * asociación @ManyToOne/@OneToMany que no se haya inicializado
 * explícitamente dentro de la transacción lanza LazyInitializationException
 * al armar el JSON.
 *
 * <p>Con la configuración por defecto (sin activar FORCE_LAZY_LOADING),
 * Hibernate6Module serializa una asociación no inicializada como
 * {@code null}/vacía en lugar de lanzar la excepción. Es una salvaguarda:
 * los endpoints de detalle (`obtenerPorId`) siguen usando consultas con
 * fetch join explícito para devolver los datos completos; este módulo
 * evita que la aplicación se caiga si algún endpoint nuevo olvida hacerlo.</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        return new Hibernate6Module();
    }
}
