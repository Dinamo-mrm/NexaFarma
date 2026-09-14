package com.nexafarma.controller;

import com.nexafarma.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    private LocalDate defDesde(LocalDate desde) {
        return desde != null ? desde : LocalDate.now().withDayOfMonth(1);
    }

    private LocalDate defHasta(LocalDate hasta) {
        return hasta != null ? hasta : LocalDate.now();
    }

    @GetMapping("/ventas/resumen")
    public ResponseEntity<Map<String, Object>> resumenVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.resumenVentas(defDesde(desde), defHasta(hasta)));
    }

    @GetMapping("/ventas/detalle")
    public ResponseEntity<List<Map<String, Object>>> ventasDetalle(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.ventasDetalle(defDesde(desde), defHasta(hasta)));
    }

    @GetMapping("/ventas/metodo-pago")
    public ResponseEntity<List<Map<String, Object>>> ventasPorMetodoPago(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.ventasPorMetodoPago(defDesde(desde), defHasta(hasta)));
    }

    @GetMapping("/medicamentos/mas-vendidos")
    public ResponseEntity<List<Map<String, Object>>> medicamentosMasVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(reporteService.medicamentosMasVendidos(defDesde(desde), defHasta(hasta), limite));
    }

    @GetMapping("/clientes/frecuentes")
    public ResponseEntity<List<Map<String, Object>>> clientesFrecuentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(reporteService.clientesFrecuentes(defDesde(desde), defHasta(hasta), limite));
    }

    @GetMapping("/empleados/mayores-ventas")
    public ResponseEntity<List<Map<String, Object>>> empleadosMayoresVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(reporteService.empleadosMayoresVentas(defDesde(desde), defHasta(hasta), limite));
    }

    @GetMapping("/inventario/stock-bajo")
    public ResponseEntity<List<Map<String, Object>>> stockBajo() {
        return ResponseEntity.ok(reporteService.stockBajoYAgotado());
    }

    @GetMapping("/inventario/vencimientos")
    public ResponseEntity<List<Map<String, Object>>> vencimientos(
            @RequestParam(required = false) Integer dias) {
        return ResponseEntity.ok(reporteService.vencimientos(dias));
    }

    @GetMapping("/inventario/valorizado")
    public ResponseEntity<List<Map<String, Object>>> inventarioValorizado() {
        return ResponseEntity.ok(reporteService.inventarioValorizado());
    }

    @GetMapping("/compras/por-proveedor")
    public ResponseEntity<List<Map<String, Object>>> comprasPorProveedor(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.comprasPorProveedor(defDesde(desde), defHasta(hasta)));
    }

    @GetMapping("/ganancias")
    public ResponseEntity<Map<String, Object>> ganancias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.gananciasPeriodo(defDesde(desde), defHasta(hasta)));
    }

    @GetMapping("/formulas")
    public ResponseEntity<List<Map<String, Object>>> formulas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.formulasRegistradas(defDesde(desde), defHasta(hasta)));
    }
}
