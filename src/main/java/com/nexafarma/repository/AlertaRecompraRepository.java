package com.nexafarma.repository;

import com.nexafarma.entity.AlertaRecompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AlertaRecompraRepository extends JpaRepository<AlertaRecompra, Long> {

    List<AlertaRecompra> findByContactadoFalseAndFechaSugeridaLessThanEqual(LocalDate fecha);

    List<AlertaRecompra> findByContactadoFalseOrderByFechaSugeridaAsc();
}
