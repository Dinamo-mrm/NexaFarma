package com.nexafarma.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CompraItemRequest(
        @NotNull(message = "El medicamento es obligatorio") Long medicamentoId,
        @NotNull @Min(value = 1, message = "La cantidad debe ser mayor a 0") Integer cantidad,
        /** Opcional: si no se envía, se usa el precioCompra actual del medicamento. */
        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0") BigDecimal precioUnitario
) {
}
