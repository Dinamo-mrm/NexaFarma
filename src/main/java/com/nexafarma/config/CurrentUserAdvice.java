package com.nexafarma.config;

import com.nexafarma.entity.Usuario;
import com.nexafarma.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expone datos del usuario autenticado a todas las vistas Thymeleaf
 * (perfil en navbar, rol, nombre del empleado).
 * Usa JOIN FETCH + @Transactional para no disparar LazyInitializationException.
 */
@ControllerAdvice
public class CurrentUserAdvice {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("usuarioActual")
    @Transactional(readOnly = true)
    public Map<String, Object> usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            return Map.of("autenticado", false);
        }

        String username = auth.getName();
        Usuario u = usuarioRepository.findByUsernameWithEmpleadoYRol(username).orElse(null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("autenticado", true);
        data.put("username", username);

        if (u == null) {
            data.put("nombre", username);
            String rolAuth = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                    .collect(Collectors.joining(","));
            data.put("rol", rolAuth);
            data.put("empleadoId", 0L);
            return data;
        }

        String nombre = username;
        Long empleadoId = 0L;
        try {
            if (u.getEmpleado() != null) {
                if (u.getEmpleado().getNombreCompleto() != null) {
                    nombre = u.getEmpleado().getNombreCompleto();
                }
                if (u.getEmpleado().getId() != null) {
                    empleadoId = u.getEmpleado().getId();
                }
            }
        } catch (Exception ignored) {
            // fallback seguro si aún hubiera proxy sin sesión
            nombre = u.getUsername() != null ? u.getUsername() : username;
        }

        String rol = "";
        try {
            if (u.getRol() != null && u.getRol().getNombre() != null) {
                rol = u.getRol().getNombre().name();
            }
        } catch (Exception ignored) {
            rol = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                    .findFirst()
                    .orElse("");
        }

        data.put("nombre", nombre);
        data.put("rol", rol);
        data.put("empleadoId", empleadoId);
        return data;
    }
}
