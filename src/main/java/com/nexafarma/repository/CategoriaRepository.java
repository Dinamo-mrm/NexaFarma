package com.nexafarma.repository;

import com.nexafarma.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // --- Método ya existente en el repositorio original: se conserva sin cambios ---
    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    // --- Método nuevo, para listar de forma ordenada y predecible ---
    List<Categoria> findAllByOrderByNombreAsc();
}