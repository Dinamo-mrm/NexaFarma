package com.nexafarma.repository;

import com.nexafarma.entity.EstadoVenta;
import com.nexafarma.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByNumeroVenta(String numeroVenta);

    List<Venta> findByClienteIdOrderByFechaDesc(Long clienteId);

    List<Venta> findByEmpleadoIdAndFechaBetween(Long empleadoId, LocalDateTime desde, LocalDateTime hasta);

    Page<Venta> findByEstado(EstadoVenta estado, Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE v.fecha BETWEEN :desde AND :hasta AND v.estado = 'PAGADA'")
    List<Venta> findVentasPagadasEntre(LocalDateTime desde, LocalDateTime hasta);
}
