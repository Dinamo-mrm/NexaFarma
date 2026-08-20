package com.nexafarma.service;

import com.nexafarma.entity.Categoria;

import java.util.List;

public interface CategoriaService {

    Categoria crear(Categoria categoria);

    Categoria obtenerPorId(Long id);

    List<Categoria> listarTodas();

    Categoria actualizar(Long id, Categoria datosActualizados);

    void eliminar(Long id);
}