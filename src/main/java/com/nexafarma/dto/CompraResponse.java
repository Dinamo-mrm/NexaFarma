package com.nexafarma.dto;

import com.nexafarma.entity.Compra;
import com.nexafarma.entity.EstadoCompra;
import com.nexafarma.entity.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompraResponse(
        Long id,
        String numeroCompra,
        Long proveedorId,
        String razonSocialProveedor,
        Long empleadoResponsableId,
        String nombreEmpleadoResponsable,
        LocalDateTime fechaCompra,
        List<DetalleCompraResponse> detalles,
        BigDecimal subtotal,
        BigDecimal impuestos,
        BigDecimal total,
        MetodoPago formaPago,
        EstadoCompra estado
) {
    public static CompraResponse desde(Compra c) {
        return new CompraResponse(
                c.getId(), c.getNumeroCompra(), c.getProveedor().getId(), c.getProveedor().getRazonSocial(),
                c.getEmpleadoResponsable().getId(), c.getEmpleadoResponsable().getNombreCompleto(),
                c.getFechaCompra(),
                c.getDetalles().stream().map(DetalleCompraResponse::desde).toList(),
                c.getSubtotal(), c.getImpuestos(), c.getTotal(), c.getFormaPago(), c.getEstado()
        );
    }
}
