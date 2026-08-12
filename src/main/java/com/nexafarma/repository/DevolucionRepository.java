package com.nexafarma.repository;

import com.nexafarma.entity.Devolucion;
import com.nexafarma.entity.TipoDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

    List<Devolucion> findByTipo(TipoDevolucion tipo);

    List<Devolucion> findByValidadoPorFarmaceuticoFalse();

    List<Devolucion> findByVentaOrigenId(Long ventaId);

    List<Devolucion> findByCompraOrigenId(Long compraId);
}
