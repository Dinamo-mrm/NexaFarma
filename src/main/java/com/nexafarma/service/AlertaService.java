package com.nexafarma.service;

import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Lote;

import java.util.List;
import java.util.Map;

public interface AlertaService {

    List<Inventario> obtenerAlertasStockBajo();

    List<Lote> obtenerAlertasProximosAVencer(Integer dias);

    List<Lote> obtenerLotesVencidos();

    /** Job diario: marca vencidos y genera resumen de alertas 30/60/90. */
    Map<String, Object> ejecutarRevisionNocturna();

    /** Sugiere stock mínimo según demanda de los últimos 30 días. */
    List<Map<String, Object>> sugerirStockMinimoDinamico();

    /** Aplica sugerencias de stock mínimo (opcional, admin). */
    int aplicarSugerenciasStockMinimo();
}
