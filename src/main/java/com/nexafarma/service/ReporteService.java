package com.nexafarma.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Agregaciones de negocio para el módulo de Reportes (FarmaSoft Plus §13).
 */
public interface ReporteService {

    /** Resumen de ventas en un rango (total, cantidad, ticket promedio). */
    Map<String, Object> resumenVentas(LocalDate desde, LocalDate hasta);

    /** Listado de ventas pagadas en el rango (para tabla / export). */
    List<Map<String, Object>> ventasDetalle(LocalDate desde, LocalDate hasta);

    /** Ventas agrupadas por método de pago. */
    List<Map<String, Object>> ventasPorMetodoPago(LocalDate desde, LocalDate hasta);

    /** Medicamentos más vendidos (top N por unidades). */
    List<Map<String, Object>> medicamentosMasVendidos(LocalDate desde, LocalDate hasta, int limite);

    /** Clientes con más compras (frecuencia + monto). */
    List<Map<String, Object>> clientesFrecuentes(LocalDate desde, LocalDate hasta, int limite);

    /** Empleados con mayores ventas (monto). */
    List<Map<String, Object>> empleadosMayoresVentas(LocalDate desde, LocalDate hasta, int limite);

    /** Stock bajo y agotado. */
    List<Map<String, Object>> stockBajoYAgotado();

    /** Lotes próximos a vencer y ya vencidos. */
    List<Map<String, Object>> vencimientos(Integer diasAnticipacion);

    /** Inventario valorizado (cantidad × precio venta / compra). */
    List<Map<String, Object>> inventarioValorizado();

    /** Compras por proveedor en el rango. */
    List<Map<String, Object>> comprasPorProveedor(LocalDate desde, LocalDate hasta);

    /** Ganancias aproximadas del periodo (ventas − costo estimado). */
    Map<String, Object> gananciasPeriodo(LocalDate desde, LocalDate hasta);

    /** Fórmulas médicas registradas en el rango. */
    List<Map<String, Object>> formulasRegistradas(LocalDate desde, LocalDate hasta);
}
