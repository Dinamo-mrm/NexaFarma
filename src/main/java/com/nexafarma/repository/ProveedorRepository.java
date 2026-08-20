package com.nexafarma.repository;

import com.nexafarma.entity.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByNit(String nit);

    boolean existsByNit(String nit);

    Page<Proveedor> findByActivoTrue(Pageable pageable);

    Page<Proveedor> findByRazonSocialContainingIgnoreCaseAndActivoTrue(String razonSocial, Pageable pageable);
}