package com.nexafarma.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Agregaciones de negocio para el módulo de Reportes (FarmaSoft Plus §13).
 * Incluye ventas por periodo, ranking de productos, clientes, empleados,
 * inventario, compras por proveedor (asociado a medicamentos), movimientos
 * y fórmulas. La exportación a CSV/Excel e impresión PDF se resuelve en la UI.
 */
public interface ReporteService {

    Map<String, Object> resumenVentas(LocalDate desde, LocalDate hasta);

    /**
     * Resumen predefinido: diario (hoy), semanal (últimos 7 días) o mensual (mes en curso).
     * @param tipo DIARIO | SEMANAL | MENSUAL
     */
    Map<String, Object> resumenVentasPorTipo(String tipo);

    List<Map<String, Object>> ventasDetalle(LocalDate desde, LocalDate hasta);

    List<Map<String, Object>> ventasPorMetodoPago(LocalDate desde, LocalDate hasta);

    List<Map<String, Object>> ventasPorCategoria(LocalDate desde, LocalDate hasta);

    List<Map<String, Object>> medicamentosMasVendidos(LocalDate desde, LocalDate hasta, int limite);

    List<Map<String, Object>> medicamentosMenosVendidos(LocalDate desde, LocalDate hasta, int limite);

    List<Map<String, Object>> clientesFrecuentes(LocalDate desde, LocalDate hasta, int limite);

    List<Map<String, Object>> empleadosMayoresVentas(LocalDate desde, LocalDate hasta, int limite);

    List<Map<String, Object>> stockBajo();

    List<Map<String, Object>> productosAgotados();

    List<Map<String, Object>> proximosAVencer(Integer diasAnticipacion);

    List<Map<String, Object>> medicamentosVencidos();

    /** Compatibilidad: stock bajo + agotados juntos. */
    List<Map<String, Object>> stockBajoYAgotado();

    /** Compatibilidad: próximos + vencidos juntos. */
    List<Map<String, Object>> vencimientos(Integer diasAnticipacion);

    List<Map<String, Object>> inventarioValorizado();

    List<Map<String, Object>> movimientosInventario(LocalDate desde, LocalDate hasta);

    List<Map<String, Object>> comprasPorProveedor(LocalDate desde, LocalDate hasta);

    Map<String, Object> gananciasPeriodo(LocalDate desde, LocalDate hasta);

    List<Map<String, Object>> formulasRegistradas(LocalDate desde, LocalDate hasta);

    /** Catálogo de reportes disponibles (metadatos para la UI). */
    List<Map<String, Object>> catalogoReportes();
}
