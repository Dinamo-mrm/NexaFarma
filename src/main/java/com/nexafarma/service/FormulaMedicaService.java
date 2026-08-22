package com.nexafarma.service;

import com.nexafarma.entity.FormulaMedica;

import java.util.List;

public interface FormulaMedicaService {

    FormulaMedica crear(FormulaMedica formula);

    FormulaMedica obtenerPorId(Long id);

    List<FormulaMedica> listarPorCliente(Long clienteId);

    /**
     * Busca, entre las formulas vigentes del cliente, una que ampare el
     * medicamento indicado. Lanza ReglaNegocioException si no encuentra
     * ninguna (usado por VentaService antes de vender un medicamento
     * que requiere formula).
     */
    FormulaMedica validarFormulaParaVenta(Long clienteId, Long medicamentoId);
}
