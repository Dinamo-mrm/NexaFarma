package com.nexafarma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Base de auditoría JPA. Las entidades Venta/Compra/MovimientoInventario
 * pueden anotar campos con @CreatedBy, @LastModifiedBy, @CreatedDate, @LastModifiedDate.
 * El auditor se toma del usuario autenticado en Spring Security.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of("sistema");
            }
            return Optional.ofNullable(auth.getName());
        };
    }
}
