package com.nexafarma.exception;

/**
 * Se lanza cuando se busca por id/clave una entidad que no existe
 * (Cliente, Compra, Venta, FormulaMedica, Medicamento, etc.).
 * El manejo global (@ControllerAdvice) es responsabilidad del
 * Desarrollador 1 (Infraestructura y Seguridad); esta excepción solo
 * documenta la condición de negocio para que ese handler la traduzca
 * a un HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException de(String entidad, Long id) {
        return new RecursoNoEncontradoException(entidad + " con id " + id + " no fue encontrado(a)");
    }
}
