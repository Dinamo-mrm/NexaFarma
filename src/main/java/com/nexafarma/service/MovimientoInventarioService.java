package com.nexafarma.service;

import com.nexafarma.entity.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovimientoInventarioService {

    /** Entrada de inventario (ej: recepción de compra). Aumenta Lote e Inventario. */
    MovimientoInventario registrarEntrada(Long loteId, Integer cantidad, Long empleadoResponsableId, String motivo);

    /** Salida de inventario (ej: venta). Valida que el lote esté vigente y con stock suficiente. */
    MovimientoInventario registrarSalida(Long loteId, Integer cantidad, Long empleadoResponsableId, String motivo);

    Page<MovimientoInventario> listarPorMedicamento(Long medicamentoId, Pageable pageable);
}