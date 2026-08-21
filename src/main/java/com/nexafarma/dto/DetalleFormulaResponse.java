package com.nexafarma.dto;

import com.nexafarma.entity.DetalleFormula;

public record DetalleFormulaResponse(
        Long id,
        Long medicamentoId,
        String nombreMedicamento,
        String dosis,
        String frecuencia,
        String duracionTratamiento
) {
    public static DetalleFormulaResponse desde(DetalleFormula d) {
        return new DetalleFormulaResponse(
                d.getId(), d.getMedicamento().getId(), d.getMedicamento().getNombreComercial(),
                d.getDosis(), d.getFrecuencia(), d.getDuracionTratamiento()
        );
    }
}
