package com.nexafarma.repository;

import com.nexafarma.entity.FormulaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormulaMedicaRepository extends JpaRepository<FormulaMedica, Long> {

    List<FormulaMedica> findByClienteIdOrderByFechaExpedicionDesc(Long clienteId);
}
