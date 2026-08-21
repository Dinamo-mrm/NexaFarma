package com.nexafarma.exception;

/**
 * Se lanza cuando, al procesar una Venta, la suma de existencias en lotes
 * activos (no vencidos) de un medicamento es menor a la cantidad solicitada.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
