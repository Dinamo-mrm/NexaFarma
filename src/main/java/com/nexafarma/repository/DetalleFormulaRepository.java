package com.nexafarma.repository;

import com.nexafarma.entity.DetalleFormula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleFormulaRepository extends JpaRepository<DetalleFormula, Long> {

    List<DetalleFormula> findByFormulaMedicaId(Long formulaMedicaId);

    List<DetalleFormula> findByMedicamentoId(Long medicamentoId);
}
