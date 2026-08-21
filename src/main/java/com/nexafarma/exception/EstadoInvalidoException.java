package com.nexafarma.exception;

/**
 * Se lanza cuando se intenta una transición de estado no permitida, por
 * ejemplo anular una Venta que ya está ANULADA, o recibir una Compra que
 * ya fue CANCELADA.
 */
public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
