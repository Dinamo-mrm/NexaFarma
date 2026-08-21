package com.nexafarma.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LoteRecibidoRequest(
        @NotNull(message = "El detalle de compra es obligatorio") Long detalleCompraId,
        @NotBlank(message = "El número de lote es obligatorio") String numeroLote,
        @NotNull(message = "La fecha de fabricación es obligatoria") LocalDate fechaFabricacion,
        @NotNull(message = "La fecha de vencimiento es obligatoria") LocalDate fechaVencimiento,
        @NotNull @Min(value = 1, message = "La cantidad recibida debe ser mayor a 0") Integer cantidad
) {
}
