package com.nexafarma.repository;

import com.nexafarma.entity.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByMedicamentoId(Long medicamentoId);

    @Query("SELECT i FROM Inventario i JOIN FETCH i.medicamento WHERE i.cantidadDisponible <= i.stockMinimo")
    List<Inventario> findConStockBajo();

    @Query("SELECT i FROM Inventario i JOIN FETCH i.medicamento WHERE i.cantidadDisponible <= 0")
    List<Inventario> findAgotados();
}
