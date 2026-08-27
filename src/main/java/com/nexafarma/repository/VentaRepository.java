package com.nexafarma.repository;

import com.nexafarma.entity.EstadoVenta;
import com.nexafarma.entity.Venta;
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
public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByNumeroVenta(String numeroVenta);

    List<Venta> findByClienteIdOrderByFechaDesc(Long clienteId);

    List<Venta> findByEmpleadoIdAndFechaBetween(Long empleadoId, LocalDateTime desde, LocalDateTime hasta);

    // Las asociaciones son LAZY y open-in-view=false: sin el fetch join de
    // cliente/empleado aca, el controlador REST (que devuelve la entidad tal
    // cual) lanza LazyInitializationException al armar el JSON de cada fila.
    @Override
    @EntityGraph(attributePaths = {"cliente", "empleado"})
    Page<Venta> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "empleado"})
    Page<Venta> findByEstado(EstadoVenta estado, Pageable pageable);

    /**
     * Version "completa" para endpoints de detalle: trae cliente, empleado
     * y cada DetalleVenta con su medicamento y lote ya inicializados, para
     * que la respuesta JSON no dependa de lazy-loading fuera de la
     * transaccion.
     */
    @Query("SELECT v FROM Venta v " +
            "JOIN FETCH v.cliente " +
            "JOIN FETCH v.empleado " +
            "LEFT JOIN FETCH v.detalles d " +
            "LEFT JOIN FETCH d.medicamento " +
            "LEFT JOIN FETCH d.lote " +
            "WHERE v.id = :id")
    Optional<Venta> findDetalladaById(Long id);

    @Query("SELECT v FROM Venta v WHERE v.fecha BETWEEN :desde AND :hasta AND v.estado = 'PAGADA'")
    List<Venta> findVentasPagadasEntre(LocalDateTime desde, LocalDateTime hasta);
}

