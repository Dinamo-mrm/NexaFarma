package com.nexafarma.dto;

import com.nexafarma.entity.FormulaMedica;

import java.time.LocalDate;
import java.util.List;

public record FormulaMedicaResponse(
        Long id,
        Long clienteId,
        String nombreCliente,
        String nombreMedico,
        String numeroTarjetaProfesional,
        String entidadSalud,
        LocalDate fechaExpedicion,
        Integer duracionTratamientoDias,
        boolean vigente,
        String archivoAdjuntoUrl,
        List<DetalleFormulaResponse> detalles
) {
    public static FormulaMedicaResponse desde(FormulaMedica f) {
        return new FormulaMedicaResponse(
                f.getId(), f.getCliente().getId(), f.getCliente().getNombreCompleto(),
                f.getNombreMedico(), f.getNumeroTarjetaProfesional(), f.getEntidadSalud(),
                f.getFechaExpedicion(), f.getDuracionTratamientoDias(), f.esVigente(),
                f.getArchivoAdjuntoUrl(),
                f.getDetalles().stream().map(DetalleFormulaResponse::desde).toList()
        );
    }
}
