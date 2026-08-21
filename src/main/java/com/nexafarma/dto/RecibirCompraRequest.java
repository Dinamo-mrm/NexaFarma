package com.nexafarma.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Datos de recepción física de una Compra: por cada DetalleCompra se indica
 * el lote en el que ingresa la mercancía (número de lote, fechas). Esta
 * información no puede inferirse al momento de generar la orden de compra,
 * por eso se captura en un paso independiente ("recibir").
 */
public record RecibirCompraRequest(
        @NotEmpty(message = "Debe informar al menos un lote recibido") @Valid List<LoteRecibidoRequest> lotesRecibidos
) {
}
