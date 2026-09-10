package com.nexafarma.config;

import com.nexafarma.service.RolService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Garantiza que los cuatro roles requeridos existan antes de crear usuarios. */
@Configuration
public class RolesInicialesConfig {

    @Bean
    CommandLineRunner inicializarRoles(RolService rolService) {
        return args -> rolService.inicializarRolesPorDefecto();
    }
}
