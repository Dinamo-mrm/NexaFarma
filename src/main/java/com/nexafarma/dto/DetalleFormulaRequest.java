package com.nexafarma.dto;

import jakarta.validation.constraints.NotNull;

public record DetalleFormulaRequest(
        @NotNull(message = "El medicamento es obligatorio") Long medicamentoId,
        String dosis,
        String frecuencia,
        String duracionTratamiento
) {
}
