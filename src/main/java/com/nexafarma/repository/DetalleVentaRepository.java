package com.nexafarma.repository;

import com.nexafarma.entity.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    List<DetalleVenta> findByVentaId(Long ventaId);

    @Query("""
            SELECT dv.medicamento.id, dv.medicamento.nombreComercial, SUM(dv.cantidad) as totalVendido
            FROM DetalleVenta dv
            GROUP BY dv.medicamento.id, dv.medicamento.nombreComercial
            ORDER BY totalVendido DESC
            """)
    List<Object[]> findMedicamentosMasVendidos();

    @Query("""
            SELECT dv.medicamento.id, SUM(dv.cantidad)
            FROM DetalleVenta dv
            JOIN dv.venta v
            WHERE v.fecha >= :desde AND v.estado = 'PAGADA'
            GROUP BY dv.medicamento.id
            """)
    List<Object[]> sumarCantidadesVendidasDesde(@Param("desde") LocalDateTime desde);
}
