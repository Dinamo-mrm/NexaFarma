package com.nexafarma.service;

import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Lote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de agregación de alertas. No duplica consultas: reutiliza
 * InventarioService (stock bajo) y LoteService (proximos a vencer), que ya
 * existen desde los Puntos 5 y 6.
 */
@Service
@Transactional(readOnly = true)
public class AlertaServiceImpl implements AlertaService {

    private final InventarioService inventarioService;
    private final LoteService loteService;

    public AlertaServiceImpl(InventarioService inventarioService, LoteService loteService) {
        this.inventarioService = inventarioService;
        this.loteService = loteService;
    }

    @Override
    public List<Inventario> obtenerAlertasStockBajo() {
        return inventarioService.listarStockBajo();
    }

    @Override
    public List<Lote> obtenerAlertasProximosAVencer(Integer dias) {
        return loteService.listarProximosAVencer(dias);
    }
}