package com.nexafarma.repository;

import com.nexafarma.entity.EstadoLote;
import com.nexafarma.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    // --- Métodos ya existentes en el repositorio original: se conservan sin cambios ---

    List<Lote> findByMedicamentoIdAndEstado(Long medicamentoId, EstadoLote estado);

    List<Lote> findByMedicamentoIdAndEstadoOrderByFechaVencimientoAsc(Long medicamentoId, EstadoLote estado);

    @Query("SELECT l FROM Lote l WHERE l.fechaVencimiento <= :fechaLimite AND l.estado = 'ACTIVO'")
    List<Lote> findProximosAVencer(@Param("fechaLimite") LocalDate fechaLimite);

    @Query("SELECT l FROM Lote l WHERE l.fechaVencimiento < CURRENT_DATE AND l.estado = 'ACTIVO'")
    List<Lote> findVencidosNoActualizados();

    // --- Método nuevo, necesario para evitar lotes duplicados por medicamento ---

    boolean existsByMedicamentoIdAndNumeroLote(Long medicamentoId, String numeroLote);
}