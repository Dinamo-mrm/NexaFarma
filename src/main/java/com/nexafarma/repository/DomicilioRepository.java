package com.nexafarma.repository;

import com.nexafarma.entity.Domicilio;
import com.nexafarma.entity.EstadoDomicilio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomicilioRepository extends JpaRepository<Domicilio, Long> {

    Optional<Domicilio> findByVentaId(Long ventaId);

    List<Domicilio> findByEstado(EstadoDomicilio estado);

    List<Domicilio> findByDomiciliarioIdAndEstado(Long domiciliarioId, EstadoDomicilio estado);
}
