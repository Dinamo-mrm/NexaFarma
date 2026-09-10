package com.nexafarma.security;

import com.nexafarma.entity.Empleado;
import com.nexafarma.entity.Rol;
import com.nexafarma.entity.RolNombre;
import com.nexafarma.entity.Usuario;
import com.nexafarma.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void cargaCredencialesYAutoridadDelRol() {
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario(true, true)));

        UserDetails resultado = usuarioDetailsService.loadUserByUsername("ana");

        assertThat(resultado.getUsername()).isEqualTo("ana");
        assertThat(resultado.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(resultado.isEnabled()).isTrue();
        assertThat(resultado.getAuthorities()).extracting("authority").containsExactly("ROLE_VENDEDOR");
    }

    @Test
    void deshabilitaElAccesoSiElUsuarioOEmpleadoEstaInactivo() {
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario(false, true)));

        assertThat(usuarioDetailsService.loadUserByUsername("ana").isEnabled()).isFalse();
    }

    @Test
    void rechazaUnUsernameInexistente() {
        when(usuarioRepository.findByUsername("desconocido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioDetailsService.loadUserByUsername("desconocido"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private Usuario usuario(boolean usuarioActivo, boolean empleadoActivo) {
        return Usuario.builder()
                .username("ana")
                .passwordHash("hash-bcrypt")
                .activo(usuarioActivo)
                .empleado(Empleado.builder().activo(empleadoActivo).build())
                .rol(Rol.builder().nombre(RolNombre.VENDEDOR).build())
                .build();
    }
}
