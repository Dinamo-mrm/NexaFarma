package com.nexafarma.repository;

import com.nexafarma.entity.Compra;
import com.nexafarma.entity.EstadoCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    Optional<Compra> findByNumeroCompra(String numeroCompra);

    List<Compra> findByProveedorIdOrderByFechaCompraDesc(Long proveedorId);

    // Ver nota en VentaRepository.findAll: evita LazyInitializationException
    // al serializar la entidad directamente en el controlador REST.
    @Override
    @EntityGraph(attributePaths = {"proveedor", "empleadoResponsable"})
    Page<Compra> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"proveedor", "empleadoResponsable"})
    Page<Compra> findByEstado(EstadoCompra estado, Pageable pageable);

    @Query("SELECT c FROM Compra c " +
            "JOIN FETCH c.proveedor " +
            "JOIN FETCH c.empleadoResponsable " +
            "LEFT JOIN FETCH c.detalles d " +
            "LEFT JOIN FETCH d.medicamento " +
            "WHERE c.id = :id")
    Optional<Compra> findDetalladaById(Long id);

    List<Compra> findByFechaCompraBetween(LocalDateTime desde, LocalDateTime hasta);
}

