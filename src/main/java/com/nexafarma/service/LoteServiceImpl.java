package com.nexafarma.service;

import com.nexafarma.entity.EstadoLote;
import com.nexafarma.entity.Lote;
import com.nexafarma.exception.DuplicateResourceException;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.LoteRepository;
import com.nexafarma.repository.MedicamentoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class LoteServiceImpl implements LoteService {

    private final LoteRepository loteRepository;
    private final MedicamentoRepository medicamentoRepository;

    @Value("${nexafarma.lotes.dias-proximos-vencer:30}")
    private int diasProximosVencerPorDefecto;

    public LoteServiceImpl(LoteRepository loteRepository, MedicamentoRepository medicamentoRepository) {
        this.loteRepository = loteRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    @Override
    public Lote crear(Lote lote) {
        if (lote.getMedicamento() == null || lote.getMedicamento().getId() == null) {
            throw new ResourceNotFoundException("Debe indicar el medicamento del lote");
        }
        medicamentoRepository.findById(lote.getMedicamento().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medicamento no encontrado con id " + lote.getMedicamento().getId()));

        if (loteRepository.existsByMedicamentoIdAndNumeroLote(lote.getMedicamento().getId(), lote.getNumeroLote())) {
            throw new DuplicateResourceException(
                    "Ya existe el lote " + lote.getNumeroLote() + " para este medicamento");
        }
        if (lote.getFechaVencimiento() != null && lote.getFechaFabricacion() != null
                && !lote.getFechaVencimiento().isAfter(lote.getFechaFabricacion())) {
            throw new ReglaNegocioException("La fecha de vencimiento debe ser posterior a la de fabricación");
        }

        lote.setId(null);
        lote.setEstado(EstadoLote.ACTIVO);
        return loteRepository.save(lote);
    }

    @Override
    @Transactional(readOnly = true)
    public Lote obtenerPorId(Long id) {
        return loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> listarPorMedicamento(Long medicamentoId) {
        return loteRepository.findByMedicamentoIdAndEstado(medicamentoId, EstadoLote.ACTIVO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> listarActivosFefo(Long medicamentoId) {
        return loteRepository.findByMedicamentoIdAndEstadoOrderByFechaVencimientoAsc(medicamentoId, EstadoLote.ACTIVO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> listarVencidos() {
        return loteRepository.findVencidosNoActualizados();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> listarProximosAVencer(Integer dias) {
        int diasEfectivos = (dias != null) ? dias : diasProximosVencerPorDefecto;
        LocalDate fechaLimite = LocalDate.now().plusDays(diasEfectivos);
        return loteRepository.findProximosAVencer(fechaLimite);
    }

    @Override
    @Transactional(readOnly = true)
    public void validarLoteVigente(Long loteId) {
        Lote lote = obtenerPorId(loteId);
        if (lote.getEstado() != EstadoLote.ACTIVO) {
            throw new ReglaNegocioException("El lote " + lote.getNumeroLote() + " no está activo");
        }
        if (lote.estaVencido()) {
            throw new ReglaNegocioException(
                    "El lote " + lote.getNumeroLote() + " está vencido (venció el " + lote.getFechaVencimiento() + ")");
        }
    }

    @Override
    public Lote actualizarCantidadDisponible(Long loteId, int nuevaCantidad) {
        Lote lote = obtenerPorId(loteId);
        if (nuevaCantidad < 0) {
            throw new ReglaNegocioException("La cantidad disponible del lote no puede ser negativa");
        }
        lote.setCantidadDisponible(nuevaCantidad);
        return loteRepository.save(lote);
    }
}