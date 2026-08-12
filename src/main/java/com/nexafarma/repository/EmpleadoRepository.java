package com.nexafarma.repository;

import com.nexafarma.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    Optional<Empleado> findByDocumento(String documento);

    boolean existsByDocumento(String documento);
}
