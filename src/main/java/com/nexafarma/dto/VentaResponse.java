package com.nexafarma.dto;

import com.nexafarma.entity.EstadoVenta;
import com.nexafarma.entity.MetodoPago;
import com.nexafarma.entity.Venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(
        Long id,
        String numeroVenta,
        Long clienteId,
        String nombreCliente,
        Long empleadoId,
        String nombreEmpleado,
        LocalDateTime fecha,
        List<DetalleVentaResponse> detalles,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal impuestos,
        BigDecimal total,
        MetodoPago metodoPago,
        EstadoVenta estado
) {
    public static VentaResponse desde(Venta v) {
        return new VentaResponse(
                v.getId(), v.getNumeroVenta(), v.getCliente().getId(), v.getCliente().getNombreCompleto(),
                v.getEmpleado().getId(), v.getEmpleado().getNombreCompleto(), v.getFecha(),
                v.getDetalles().stream().map(DetalleVentaResponse::desde).toList(),
                v.getSubtotal(), v.getDescuento(), v.getImpuestos(), v.getTotal(),
                v.getMetodoPago(), v.getEstado()
        );
    }
}
