package com.nexafarma.service;

import com.nexafarma.entity.MovimientoInventario;
import com.nexafarma.entity.TipoMovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovimientoInventarioService {

    MovimientoInventario registrarEntrada(Long loteId, Integer cantidad, Long empleadoResponsableId, String motivo);

    MovimientoInventario registrarSalida(Long loteId, Integer cantidad, Long empleadoResponsableId, String motivo);

    /**
     * Movimiento sin lote específico (devoluciones, bajas).
     * deltaSigno implícito en el tipo: ENTRADA suma, SALIDA/SALIDA_BAJA no altera stock aquí
     * (el caller ya ajustó Inventario si corresponde).
     */
    MovimientoInventario registrarAjusteSinLote(Long medicamentoId, Integer cantidad,
                                                 TipoMovimientoInventario tipo,
                                                 Long empleadoResponsableId, String motivo);

    Page<MovimientoInventario> listarPorMedicamento(Long medicamentoId, Pageable pageable);
}
