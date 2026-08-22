package com.nexafarma.service;

import com.nexafarma.entity.Empleado;
import com.nexafarma.entity.RolNombre;
import com.nexafarma.entity.Usuario;
import com.nexafarma.exception.DuplicateResourceException;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final RolService rolService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                               EmpleadoRepository empleadoRepository,
                               RolService rolService,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.rolService = rolService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Usuario crear(Long empleadoId, String username, String passwordPlano, RolNombre rol) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id " + empleadoId));

        if (usuarioRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Ya existe un usuario con el username " + username);
        }
        if (usuarioRepository.findByEmpleadoId(empleadoId).isPresent()) {
            throw new DuplicateResourceException("El empleado " + empleadoId + " ya tiene un usuario asignado");
        }
        if (passwordPlano == null || passwordPlano.length() < 8) {
            throw new ReglaNegocioException("La contrasena debe tener al menos 8 caracteres");
        }

        Usuario usuario = Usuario.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(passwordPlano))
                .empleado(empleado)
                .rol(rolService.obtenerPorNombre(rol))
                .activo(true)
                .build();

        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }

    @Override
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva) {
        Usuario usuario = obtenerPorId(usuarioId);
        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contrasena actual no es correcta");
        }
        if (passwordNueva == null || passwordNueva.length() < 8) {
            throw new ReglaNegocioException("La nueva contrasena debe tener al menos 8 caracteres");
        }
        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
    }

    @Override
    public void activarODesactivar(Long usuarioId, boolean activo) {
        Usuario usuario = obtenerPorId(usuarioId);
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }

    @Override
    public void registrarAcceso(Long usuarioId) {
        Usuario usuario = obtenerPorId(usuarioId);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }
}
