package com.nexafarma.repository;

import com.nexafarma.entity.MovimientoInventario;
import com.nexafarma.entity.TipoMovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    Page<MovimientoInventario> findByMedicamentoIdOrderByFechaDesc(Long medicamentoId, Pageable pageable);

    List<MovimientoInventario> findByTipoMovimientoAndFechaBetween(
            TipoMovimientoInventario tipo, LocalDateTime desde, LocalDateTime hasta);
}
