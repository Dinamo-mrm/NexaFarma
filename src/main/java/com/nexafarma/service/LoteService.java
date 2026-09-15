package com.nexafarma.service;

import com.nexafarma.entity.Lote;

import java.util.List;

public interface LoteService {

    Lote crear(Lote lote);

    /**
     * Crea un lote en estado CUARENTENA (recepción de compra).
     * No es vendible hasta liberarCuarentena.
     */
    Lote crearEnCuarentena(Lote lote);

    Lote obtenerPorId(Long id);

    List<Lote> listarPorMedicamento(Long medicamentoId);

    /** Lotes ACTIVO ordenados FEFO (primero el más próximo a vencer). */
    List<Lote> listarActivosFefo(Long medicamentoId);

    List<Lote> listarVencidos();

    List<Lote> listarProximosAVencer(Integer dias);

    /** Lotes pendientes de validación del Regente. */
    List<Lote> listarEnCuarentena();

    /**
     * Pasa el lote de CUARENTENA a ACTIVO tras validación física del Regente.
     * Solo entonces entra al stock vendible (FEFO).
     */
    Lote liberarCuarentena(Long loteId, Long empleadoRegenteId);

    void validarLoteVigente(Long loteId);

    Lote actualizarCantidadDisponible(Long loteId, int nuevaCantidad);
}
