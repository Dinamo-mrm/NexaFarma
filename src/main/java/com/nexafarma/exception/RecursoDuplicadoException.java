package com.nexafarma.exception;

/**
 * Se lanza al intentar registrar un dato que debe ser único y ya existe
 * (documento de cliente, numero de compra/venta, etc.).
 */
public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
