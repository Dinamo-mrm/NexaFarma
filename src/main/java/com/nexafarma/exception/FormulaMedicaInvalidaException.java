package com.nexafarma.exception;

/**
 * Regla de negocio central del módulo de Ventas: un medicamento marcado
 * como {@code requiereFormula} no puede venderse sin una FormulaMedica
 * vigente que lo ampare. Se lanza cuando falta la fórmula, está vencida,
 * o no incluye el medicamento que se intenta vender.
 */
public class FormulaMedicaInvalidaException extends RuntimeException {

    public FormulaMedicaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
