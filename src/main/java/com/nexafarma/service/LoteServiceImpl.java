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
        prepararNuevoLote(lote);
        // Creación manual / ajustes: por defecto ACTIVO (compatibilidad).
        if (lote.getEstado() == null) {
            lote.setEstado(EstadoLote.ACTIVO);
        }
        return loteRepository.save(lote);
    }

    @Override
    public Lote crearEnCuarentena(Lote lote) {
        prepararNuevoLote(lote);
        lote.setEstado(EstadoLote.CUARENTENA);
        return loteRepository.save(lote);
    }

    private void prepararNuevoLote(Lote lote) {
        if (lote.getMedicamento() == null || lote.getMedicamento().getId() == null) {
            throw new ResourceNotFoundException("Debe indicar el medicamento del lote");
        }
        medicamentoRepository.findById(lote.getMedicamento().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medicamento no encontrado con id " + lote.getMedicamento().getId()));

        if (loteRepository.existsByMedicamentoIdAndNumeroLote(
                lote.getMedicamento().getId(), lote.getNumeroLote())) {
            throw new DuplicateResourceException(
                    "Ya existe el lote " + lote.getNumeroLote() + " para este medicamento");
        }
        if (lote.getFechaVencimiento() != null && lote.getFechaFabricacion() != null
                && !lote.getFechaVencimiento().isAfter(lote.getFechaFabricacion())) {
            throw new ReglaNegocioException(
                    "La fecha de vencimiento debe ser posterior a la de fabricación");
        }
        lote.setId(null);
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
        return loteRepository.findByMedicamentoIdAndEstadoOrderByFechaVencimientoAsc(
                medicamentoId, EstadoLote.ACTIVO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> listarVencidos() {
        return loteRepository.findVencidosNoActualizados();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> listarProximosAVencer(Integer dias) {
        int d = dias != null ? dias : diasProximosVencerPorDefecto;
        return loteRepository.findProximosAVencer(LocalDate.now().plusDays(d));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> listarEnCuarentena() {
        return loteRepository.findByEstado(EstadoLote.CUARENTENA);
    }

    @Override
    public Lote liberarCuarentena(Long loteId, Long empleadoRegenteId) {
        if (empleadoRegenteId == null) {
            throw new ReglaNegocioException("Debe indicar el empleado Regente que valida el lote");
        }
        Lote lote = obtenerPorId(loteId);
        if (lote.getEstado() != EstadoLote.CUARENTENA) {
            throw new ReglaNegocioException(
                    "Solo se pueden liberar lotes en CUARENTENA (actual: " + lote.getEstado() + ")");
        }
        if (lote.estaVencido()) {
            lote.setEstado(EstadoLote.VENCIDO);
            loteRepository.save(lote);
            throw new ReglaNegocioException(
                    "El lote " + lote.getNumeroLote() + " ya está vencido; no puede liberarse a venta");
        }
        lote.setEstado(EstadoLote.ACTIVO);
        return loteRepository.save(lote);
    }

    @Override
    public void validarLoteVigente(Long loteId) {
        Lote lote = obtenerPorId(loteId);
        if (lote.getEstado() != EstadoLote.ACTIVO) {
            throw new ReglaNegocioException(
                    "El lote " + lote.getNumeroLote() + " no está ACTIVO (estado: " + lote.getEstado() + ")");
        }
        if (lote.estaVencido()) {
            throw new ReglaNegocioException(
                    "El lote " + lote.getNumeroLote() + " está vencido y no puede usarse en venta");
        }
    }

    @Override
    public Lote actualizarCantidadDisponible(Long loteId, int nuevaCantidad) {
        if (nuevaCantidad < 0) {
            throw new ReglaNegocioException("La cantidad disponible no puede ser negativa");
        }
        Lote lote = obtenerPorId(loteId);
        lote.setCantidadDisponible(nuevaCantidad);
        return loteRepository.save(lote);
    }
}
