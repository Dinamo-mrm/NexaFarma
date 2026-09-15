package com.nexafarma.entity;

/**
 * Ciclo de vida del lote.
 * CUARENTENA: mercancía recibida pendiente de validación del Regente (no vendible).
 * ACTIVO: disponible para venta (FEFO).
 * VENCIDO / RETIRADO: bloqueados para venta.
 */
public enum EstadoLote {
    CUARENTENA,
    ACTIVO,
    VENCIDO,
    RETIRADO
}
