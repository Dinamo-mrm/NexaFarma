package com.nexafarma.repository;

import com.nexafarma.entity.Compra;
import com.nexafarma.entity.EstadoCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    Optional<Compra> findByNumeroCompra(String numeroCompra);

    List<Compra> findByProveedorIdOrderByFechaCompraDesc(Long proveedorId);

    Page<Compra> findByEstado(EstadoCompra estado, Pageable pageable);

    List<Compra> findByFechaCompraBetween(LocalDateTime desde, LocalDateTime hasta);
}
