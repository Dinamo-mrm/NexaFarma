package com.nexafarma.service;

import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.InventarioRepository;
import com.nexafarma.repository.MedicamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MedicamentoRepository medicamentoRepository;

    public InventarioServiceImpl(InventarioRepository inventarioRepository,
                                 MedicamentoRepository medicamentoRepository) {
        this.inventarioRepository = inventarioRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Inventario obtenerPorMedicamento(Long medicamentoId) {
        return inventarioRepository.findByMedicamentoId(medicamentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay inventario registrado para el medicamento id " + medicamentoId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventario> listarTodos() {
        return inventarioRepository.findAllConMedicamento();
    }

    @Override
    public List<Inventario> listarStockBajo() {
        return inventarioRepository.findConStockBajo();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventario> listarAgotados() {
        return inventarioRepository.findAgotados();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneStockSuficiente(Long medicamentoId, int cantidadRequerida) {
        return inventarioRepository.findByMedicamentoId(medicamentoId)
                .map(inv -> inv.getCantidadDisponible() != null && inv.getCantidadDisponible() >= cantidadRequerida)
                .orElse(false);
    }

    @Override
    public Inventario ajustarCantidad(Long medicamentoId, int delta) {
        Inventario inventario = inventarioRepository.findByMedicamentoId(medicamentoId)
                .orElseGet(() -> crearInventarioInicial(medicamentoId));

        int nuevaCantidad = inventario.getCantidadDisponible() + delta;
        if (nuevaCantidad < 0) {
            throw new ReglaNegocioException("El ajuste dejaría el inventario en cantidad negativa");
        }
        inventario.setCantidadDisponible(nuevaCantidad);
        return inventarioRepository.save(inventario);
    }

    private Inventario crearInventarioInicial(Long medicamentoId) {
        Medicamento medicamento = medicamentoRepository.findById(medicamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento no encontrado con id " + medicamentoId));
        Inventario nuevo = Inventario.builder()
                .medicamento(medicamento)
                .cantidadDisponible(0)
                .stockMinimo(medicamento.getStockMinimo() != null ? medicamento.getStockMinimo() : 10)
                .build();
        return inventarioRepository.save(nuevo);
    }
}