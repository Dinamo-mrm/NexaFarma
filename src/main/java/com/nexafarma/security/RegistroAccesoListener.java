package com.nexafarma.security;

import com.nexafarma.repository.UsuarioRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Actualiza la fecha de ultimo acceso tras una autenticacion satisfactoria. */
@Component
public class RegistroAccesoListener {

    private final UsuarioRepository usuarioRepository;

    public RegistroAccesoListener(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @EventListener
    @Transactional
    public void registrarAcceso(AuthenticationSuccessEvent evento) {
        usuarioRepository.findByUsername(evento.getAuthentication().getName()).ifPresent(usuario -> {
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);
        });
    }
}
