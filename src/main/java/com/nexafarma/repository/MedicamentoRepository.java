package com.nexafarma.repository;

import com.nexafarma.entity.Medicamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    // --- Métodos ya existentes en el repositorio original: se conservan sin cambios ---

    Optional<Medicamento> findByCodigoBarras(String codigoBarras);

    Optional<Medicamento> findByCodigoInterno(String codigoInterno);

    boolean existsByCodigoBarras(String codigoBarras);

    Page<Medicamento> findByNombreComercialContainingIgnoreCaseAndActivoTrue(String nombre, Pageable pageable);

    List<Medicamento> findByCategoriaIdAndActivoTrue(Long categoriaId);

    List<Medicamento> findByProveedorIdAndActivoTrue(Long proveedorId);

    List<Medicamento> findByRequiereFormulaTrueAndActivoTrue();

    List<Medicamento> findByRequiereRefrigeracionTrueAndActivoTrue();

    @Query("""
            SELECT m FROM Medicamento m
            JOIN Inventario i ON i.medicamento = m
            WHERE i.cantidadDisponible <= i.stockMinimo AND m.activo = true
            """)
    List<Medicamento> findConStockBajo();

    // --- Métodos nuevos, necesarios para el CRUD y validación de duplicados ---

    /** Simétrico a existsByCodigoBarras: codigoInterno también es unique en Producto. */
    boolean existsByCodigoInterno(String codigoInterno);

    /** Listado paginado general de medicamentos activos, sin filtro de nombre. */
    Page<Medicamento> findByActivoTrue(Pageable pageable);
}