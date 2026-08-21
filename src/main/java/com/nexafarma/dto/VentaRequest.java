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
        @NotNull(message = "El método de pago es obligatorio") MetodoPago metodoPago,
        @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo") BigDecimal descuento,
        /**
         * Obligatorio solo si alguno de los medicamentos del carrito tiene
         * requiereFormula = true. Se valida en el service que la fórmula
         * esté vigente y ampare esos medicamentos.
         */
        Long formulaMedicaId,
        @NotEmpty(message = "La venta debe tener al menos un item") @Valid List<VentaItemRequest> items
) {
}
