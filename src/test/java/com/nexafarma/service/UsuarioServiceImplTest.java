package com.nexafarma.service;

import com.nexafarma.entity.Empleado;
import com.nexafarma.entity.Rol;
import com.nexafarma.entity.RolNombre;
import com.nexafarma.entity.Usuario;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmpleadoRepository empleadoRepository;
    @Mock
    private RolService rolService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void cifraLaContrasenaAlCrearCredenciales() {
        UsuarioServiceImpl servicio = new UsuarioServiceImpl(usuarioRepository, empleadoRepository, rolService, passwordEncoder);
        Empleado empleado = Empleado.builder().id(7L).activo(true).build();
        when(empleadoRepository.findById(7L)).thenReturn(Optional.of(empleado));
        when(usuarioRepository.existsByUsername("maria")).thenReturn(false);
        when(usuarioRepository.findByEmpleadoId(7L)).thenReturn(Optional.empty());
        when(rolService.obtenerPorNombre(RolNombre.AUXILIAR)).thenReturn(Rol.builder().nombre(RolNombre.AUXILIAR).build());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        servicio.crear(7L, "maria", "ClaveSegura1", RolNombre.AUXILIAR);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        org.mockito.Mockito.verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("ClaveSegura1");
        assertThat(passwordEncoder.matches("ClaveSegura1", captor.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void exigeLaContrasenaActualAntesDeCambiarla() {
        UsuarioServiceImpl servicio = new UsuarioServiceImpl(usuarioRepository, empleadoRepository, rolService, passwordEncoder);
        Usuario usuario = Usuario.builder().id(3L).passwordHash(passwordEncoder.encode("ClaveActual1")).build();
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> servicio.cambiarPassword(3L, "incorrecta", "NuevaClave1"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("actual");
    }
}
