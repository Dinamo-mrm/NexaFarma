package com.nexafarma.dto;

import com.nexafarma.entity.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record VentaRequest(
        @NotNull(message = "El cliente es obligatorio") Long clienteId,
        @NotNull(message = "El empleado que atiende la venta es obligatorio") Long empleadoId,
        /** Método principal; si hay varios pagos se fuerza PAGO_MIXTO. */
        MetodoPago metodoPago,
        @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo") BigDecimal descuento,
        Long formulaMedicaId,
        /** Pagos parciales (efectivo + tarjeta, etc.). Si se envía, debe cubrir el total. */
        @Valid List<PagoItemRequest> pagos,
        @NotEmpty(message = "La venta debe tener al menos un item") @Valid List<VentaItemRequest> items
) {
}
