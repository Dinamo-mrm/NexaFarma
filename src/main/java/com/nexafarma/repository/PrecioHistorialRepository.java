package com.nexafarma.repository;

import com.nexafarma.entity.PrecioHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrecioHistorialRepository extends JpaRepository<PrecioHistorial, Long> {
    List<PrecioHistorial> findByProductoIdOrderByCreadoEnDesc(Long productoId);
}
