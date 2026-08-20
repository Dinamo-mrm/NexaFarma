package com.nexafarma.service;

import com.nexafarma.entity.Lote;

import java.util.List;

public interface LoteService {

    Lote crear(Lote lote);

    Lote obtenerPorId(Long id);

    List<Lote> listarPorMedicamento(Long medicamentoId);

    /** Lotes activos de un medicamento, ordenados FEFO (primero el más próximo a vencer). */
    List<Lote> listarActivosFefo(Long medicamentoId);

    List<Lote> listarVencidos();

    /** dias == null usa el valor configurado por defecto (nexafarma.lotes.dias-proximos-vencer). */
    List<Lote> listarProximosAVencer(Integer dias);

    /** Lanza ReglaNegocioException si el lote no existe, no está ACTIVO o está vencido. */
    void validarLoteVigente(Long loteId);

    /** Uso interno de MovimientoInventarioService para ajustar cantidades tras un movimiento. */
    Lote actualizarCantidadDisponible(Long loteId, int nuevaCantidad);
}