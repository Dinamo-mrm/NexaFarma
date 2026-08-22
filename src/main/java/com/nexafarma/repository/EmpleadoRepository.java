package com.nexafarma.repository;

import com.nexafarma.entity.Empleado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    Optional<Empleado> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    Page<Empleado> findByNombreCompletoContainingIgnoreCase(String nombre, Pageable pageable);
}
