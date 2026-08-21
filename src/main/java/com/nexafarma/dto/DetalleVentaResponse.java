package com.nexafarma.dto;

import com.nexafarma.entity.DetalleVenta;

import java.math.BigDecimal;

public record DetalleVentaResponse(
        Long id,
        Long medicamentoId,
        String nombreMedicamento,
        Long loteId,
        String numeroLote,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static DetalleVentaResponse desde(DetalleVenta d) {
        return new DetalleVentaResponse(
                d.getId(), d.getMedicamento().getId(), d.getMedicamento().getNombreComercial(),
                d.getLote() != null ? d.getLote().getId() : null,
                d.getLote() != null ? d.getLote().getNumeroLote() : null,
                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()
        );
    }
}
