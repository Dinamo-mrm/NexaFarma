package com.nexafarma.dto;

import com.nexafarma.entity.DetalleCompra;

import java.math.BigDecimal;

public record DetalleCompraResponse(
        Long id,
        Long medicamentoId,
        String nombreMedicamento,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static DetalleCompraResponse desde(DetalleCompra d) {
        return new DetalleCompraResponse(
                d.getId(), d.getMedicamento().getId(), d.getMedicamento().getNombreComercial(),
                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()
        );
    }
}
