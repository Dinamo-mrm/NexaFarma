package com.nexafarma.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record FormulaMedicaRequest(
        @NotNull(message = "El cliente es obligatorio") Long clienteId,
        @NotBlank(message = "El nombre del médico es obligatorio") String nombreMedico,
        @NotBlank(message = "El número de tarjeta profesional es obligatorio") String numeroTarjetaProfesional,
        String entidadSalud,
        @NotNull(message = "La fecha de expedición es obligatoria") LocalDate fechaExpedicion,
        @NotNull @Min(value = 1, message = "La duración del tratamiento debe ser mayor a 0 días")
        Integer duracionTratamientoDias,
        String archivoAdjuntoUrl,
        @NotEmpty(message = "La fórmula debe amparar al menos un medicamento") @Valid
        List<DetalleFormulaRequest> detalles
) {
}
