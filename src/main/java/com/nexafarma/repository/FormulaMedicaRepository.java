package com.nexafarma.repository;

import com.nexafarma.entity.FormulaMedica;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormulaMedicaRepository extends JpaRepository<FormulaMedica, Long> {

    @EntityGraph(attributePaths = {"cliente", "detalles", "detalles.medicamento"})
    List<FormulaMedica> findByClienteIdOrderByFechaExpedicionDesc(Long clienteId);

    @Query("SELECT f FROM FormulaMedica f " +
            "JOIN FETCH f.cliente " +
            "LEFT JOIN FETCH f.detalles d " +
            "LEFT JOIN FETCH d.medicamento " +
            "WHERE f.id = :id")
    Optional<FormulaMedica> findDetalladaById(Long id);
}

