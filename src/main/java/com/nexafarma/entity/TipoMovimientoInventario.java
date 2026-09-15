package com.nexafarma.entity;

/**
 * ENTRADA / SALIDA / AJUSTE: movimientos operativos normales.
 * SALIDA_BAJA: merma o baja (averiado, caducado en devolución) — no reingresa a stock vendible.
 */
public enum TipoMovimientoInventario {
    ENTRADA,
    SALIDA,
    AJUSTE,
    SALIDA_BAJA
}
