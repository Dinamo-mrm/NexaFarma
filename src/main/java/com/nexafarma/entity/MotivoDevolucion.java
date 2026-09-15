package com.nexafarma.entity;

/**
 * Motivos de devolución.
 * ERROR_DIGITACION: reingresa a stock vendible (ENTRADA).
 * Producto averiado / vencido / daño: no reingresa; se registra como baja.
 */
public enum MotivoDevolucion {
    ERROR_DIGITACION,
    PRODUCTO_DEFECTUOSO,
    ERROR_EN_LA_ENTREGA,
    PRODUCTO_PROXIMO_A_VENCER,
    PRODUCTO_VENCIDO,
    RETIRO_DEL_MERCADO,
    DANO_EN_EL_EMPAQUE,
    ERROR_DEL_PROVEEDOR
}
