package com.nexafarma.service;

import com.nexafarma.entity.EstadoVenta;
import com.nexafarma.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VentaService {

    /**
     * Registra una venta completa. `venta.detalles` debe traer, por cada
     * item solicitado, el medicamento y la cantidad (precioUnitario y lote
     * se calculan/asignan automaticamente). El servicio:
     *   1. Valida que medicamentos que requieren formula tengan una vigente.
     *   2. Descuenta stock por lote en orden FEFO (primero el mas proximo a vencer).
     *   3. Bloquea automaticamente lotes vencidos (via LoteService.validarLoteVigente).
     *   4. Calcula subtotal, descuento y total.
     */
    Venta crear(Venta venta);

    Venta obtenerPorId(Long id);

    Page<Venta> listar(Pageable pageable);

    Page<Venta> listarPorEstado(EstadoVenta estado, Pageable pageable);

    /** Anula la venta y repone el stock descontado (reversa via MovimientoInventarioService). */
    void anular(Long ventaId);
}
