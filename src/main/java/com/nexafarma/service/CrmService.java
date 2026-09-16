package com.nexafarma.service;

import com.nexafarma.entity.AlertaRecompra;
import com.nexafarma.entity.Cliente;

import java.util.List;
import java.util.Map;

public interface CrmService {

    /** Acumula puntos tras una venta pagada (ignora controlados). */
    void acumularPuntosPorVenta(Long ventaId);

    /** Redime puntos; solo productos no controlados. */
    Cliente redimirPuntos(Long clienteId, int puntos);

    List<AlertaRecompra> listarRecomprasPendientes();

    AlertaRecompra marcarContactado(Long alertaId);

    /** Genera alertas de recompra a partir de fórmulas/ventas recientes. */
    int generarAlertasRecompra();
}
