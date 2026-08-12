package com.nexafarma.repository;

import com.nexafarma.entity.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    List<DetalleVenta> findByVentaId(Long ventaId);

    @Query("""
            SELECT dv.medicamento.id, SUM(dv.cantidad) as totalVendido
            FROM DetalleVenta dv
            GROUP BY dv.medicamento.id
            ORDER BY totalVendido DESC
            """)
    List<Object[]> findMedicamentosMasVendidos();
}
