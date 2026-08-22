package com.nexafarma.service;

import com.nexafarma.entity.RolNombre;
import com.nexafarma.entity.Usuario;

public interface UsuarioService {

    /** Crea el usuario de acceso de un empleado ya existente, cifrando la contrasena. */
    Usuario crear(Long empleadoId, String username, String passwordPlano, RolNombre rol);

    Usuario obtenerPorUsername(String username);

    Usuario obtenerPorId(Long id);

    /** Cambia la contrasena verificando primero la actual. */
    void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva);

    void activarODesactivar(Long usuarioId, boolean activo);

    void registrarAcceso(Long usuarioId);
}
