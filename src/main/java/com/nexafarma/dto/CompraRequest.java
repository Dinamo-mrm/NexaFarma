package com.nexafarma.dto;

import com.nexafarma.entity.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompraRequest(
        @NotNull(message = "El proveedor es obligatorio") Long proveedorId,
        @NotNull(message = "El empleado responsable es obligatorio") Long empleadoResponsableId,
        @NotNull(message = "La forma de pago es obligatoria") MetodoPago formaPago,
        @NotEmpty(message = "La compra debe tener al menos un item") @Valid List<CompraItemRequest> items
) {
}
