package com.nexafarma.repository;

import com.nexafarma.entity.MovimientoInventario;
import com.nexafarma.entity.TipoMovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    Page<MovimientoInventario> findByMedicamentoIdOrderByFechaDesc(Long medicamentoId, Pageable pageable);

    List<MovimientoInventario> findByTipoMovimientoAndFechaBetween(
            TipoMovimientoInventario tipo, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.medicamento
            JOIN FETCH m.usuarioResponsable
            WHERE m.fecha BETWEEN :desde AND :hasta
            ORDER BY m.fecha DESC
            """)
    List<MovimientoInventario> findByFechaBetweenOrderByFechaDesc(
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}

