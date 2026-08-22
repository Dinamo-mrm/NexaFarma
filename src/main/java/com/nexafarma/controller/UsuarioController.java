package com.nexafarma.controller;

import com.nexafarma.entity.RolNombre;
import com.nexafarma.entity.Usuario;
import com.nexafarma.service.UsuarioService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public record CrearUsuarioRequest(
            @NotNull Long empleadoId,
            @NotBlank String username,
            @NotBlank String password,
            @NotNull RolNombre rol) {
    }

    public record CambiarPasswordRequest(@NotBlank String passwordActual, @NotBlank String passwordNueva) {
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@RequestBody CrearUsuarioRequest request) {
        Usuario creado = usuarioService.crear(request.empleadoId(), request.username(), request.password(), request.rol());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> cambiarPassword(@PathVariable Long id, @RequestBody CambiarPasswordRequest request) {
        usuarioService.cambiarPassword(id, request.passwordActual(), request.passwordNueva());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> activarODesactivar(@PathVariable Long id, @RequestParam boolean activo) {
        usuarioService.activarODesactivar(id, activo);
        return ResponseEntity.noContent().build();
    }
}
