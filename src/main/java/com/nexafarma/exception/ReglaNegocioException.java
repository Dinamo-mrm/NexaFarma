package com.nexafarma.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción genérica para violaciones de reglas de negocio (ej: stock
 * insuficiente, lote vencido en una operación). No existía equivalente
 * previo en el proyecto.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}