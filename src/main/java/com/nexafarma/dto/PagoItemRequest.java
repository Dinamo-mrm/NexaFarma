package com.nexafarma.dto;

import com.nexafarma.entity.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagoItemRequest(
        @NotNull MetodoPago metodoPago,
        @NotNull @DecimalMin(value = "0.01", message = "El monto del pago debe ser mayor a 0") BigDecimal monto
) {
}
