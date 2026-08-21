package com.nexafarma.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VentaItemRequest(
        @NotNull(message = "El medicamento es obligatorio") Long medicamentoId,
        @NotNull @Min(value = 1, message = "La cantidad debe ser mayor a 0") Integer cantidad
) {
}
