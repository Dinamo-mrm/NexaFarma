package com.nexafarma.service;

import com.nexafarma.entity.Inventario;

import java.util.List;

public interface InventarioService {

    Inventario obtenerPorMedicamento(Long medicamentoId);

    List<Inventario> listarTodos();

    List<Inventario> listarStockBajo();

    List<Inventario> listarAgotados();

    boolean tieneStockSuficiente(Long medicamentoId, int cantidadRequerida);

    /** Uso interno de MovimientoInventarioService. Crea el registro de Inventario si no existe. */
    Inventario ajustarCantidad(Long medicamentoId, int delta);
}