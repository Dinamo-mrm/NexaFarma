package com.nexafarma.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para cuando se intenta eliminar un recurso que todavía está
 * referenciado por otras entidades (ej: Categoria con Medicamentos activos).
 * No es equivalente a DuplicateResourceException: esa es por unicidad de
 * datos (NIT, nombre); esta es por integridad referencial al eliminar.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String mensaje) {
        super(mensaje);
    }
}