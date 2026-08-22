package com.nexafarma.service;

import com.nexafarma.entity.Rol;
import com.nexafarma.entity.RolNombre;

import java.util.List;

public interface RolService {

    Rol obtenerPorNombre(RolNombre nombre);

    List<Rol> listarTodos();

    /** Crea los 4 roles del sistema si aun no existen (Administrador, Farmaceutico, Vendedor, Auxiliar). */
    void inicializarRolesPorDefecto();
}
