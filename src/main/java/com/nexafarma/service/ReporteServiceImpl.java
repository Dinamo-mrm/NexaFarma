package com.nexafarma.service;

import com.nexafarma.entity.*;
import com.nexafarma.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    private final VentaRepository ventaRepository;
    private final InventarioRepository inventarioRepository;
    private final LoteRepository loteRepository;
    private final CompraRepository compraRepository;
    private final FormulaMedicaRepository formulaMedicaRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final MedicamentoRepository medicamentoRepository;

    public ReporteServiceImpl(VentaRepository ventaRepository,
                              InventarioRepository inventarioRepository,
                              LoteRepository loteRepository,
                              CompraRepository compraRepository,
                              FormulaMedicaRepository formulaMedicaRepository,
                              MovimientoInventarioRepository movimientoInventarioRepository,
                              MedicamentoRepository medicamentoRepository) {
        this.ventaRepository = ventaRepository;
        this.inventarioRepository = inventarioRepository;
        this.loteRepository = loteRepository;
        this.compraRepository = compraRepository;
        this.formulaMedicaRepository = formulaMedicaRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    private LocalDateTime inicio(LocalDate d) {
        return d.atStartOfDay();
    }

    private LocalDateTime fin(LocalDate d) {
        return d.atTime(LocalTime.MAX);
    }

    private List<Venta> ventasConDetalle(LocalDate desde, LocalDate hasta) {
        List<Venta> base = ventaRepository.findVentasPagadasEntre(inicio(desde), fin(hasta));
        List<Venta> out = new ArrayList<>();
        for (Venta v : base) {
            ventaRepository.findDetalladaById(v.getId()).ifPresent(out::add);
        }
        return out;
    }

    private Map<String, Object> armarResumen(LocalDate desde, LocalDate hasta, List<Venta> ventas) {
        BigDecimal total = ventas.stream()
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int cantidad = ventas.size();
        BigDecimal ticket = cantidad == 0
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("desde", desde.toString());
        m.put("hasta", hasta.toString());
        m.put("cantidadVentas", cantidad);
        m.put("total", total);
        m.put("ticketPromedio", ticket);
        return m;
    }

    @Override
    public Map<String, Object> resumenVentas(LocalDate desde, LocalDate hasta) {
        List<Venta> ventas = ventaRepository.findVentasPagadasEntre(inicio(desde), fin(hasta));
        return armarResumen(desde, hasta, ventas);
    }

    @Override
    public Map<String, Object> resumenVentasPorTipo(String tipo) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde;
        LocalDate hasta = hoy;
        String t = tipo == null ? "MENSUAL" : tipo.trim().toUpperCase(Locale.ROOT);
        switch (t) {
            case "DIARIO" -> desde = hoy;
            case "SEMANAL" -> desde = hoy.minusDays(6);
            default -> {
                t = "MENSUAL";
                desde = hoy.withDayOfMonth(1);
            }
        }
        Map<String, Object> m = resumenVentas(desde, hasta);
        m.put("tipoPeriodo", t);
        return m;
    }

    @Override
    public List<Map<String, Object>> ventasDetalle(LocalDate desde, LocalDate hasta) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Venta ref : ventaRepository.findVentasPagadasEntre(inicio(desde), fin(hasta))) {
            Venta v = ventaRepository.findDetalladaById(ref.getId()).orElse(ref);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", v.getId());
            row.put("numeroVenta", v.getNumeroVenta());
            row.put("fecha", v.getFecha() != null ? v.getFecha().toString() : null);
            row.put("cliente", v.getCliente() != null ? v.getCliente().getNombreCompleto() : null);
            row.put("empleado", v.getEmpleado() != null ? v.getEmpleado().getNombreCompleto() : null);
            row.put("metodoPago", v.getMetodoPago() != null ? v.getMetodoPago().name() : null);
            row.put("total", v.getTotal());
            row.put("estado", v.getEstado() != null ? v.getEstado().name() : null);
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> ventasPorMetodoPago(LocalDate desde, LocalDate hasta) {
        Map<String, BigDecimal> montos = new LinkedHashMap<>();
        Map<String, Integer> conteos = new LinkedHashMap<>();
        for (Venta v : ventaRepository.findVentasPagadasEntre(inicio(desde), fin(hasta))) {
            String key = v.getMetodoPago() != null ? v.getMetodoPago().name() : "SIN_METODO";
            montos.merge(key, v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO, BigDecimal::add);
            conteos.merge(key, 1, Integer::sum);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (String key : montos.keySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("metodoPago", key);
            row.put("cantidad", conteos.get(key));
            row.put("total", montos.get(key));
            out.add(row);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> ventasPorCategoria(LocalDate desde, LocalDate hasta) {
        Map<String, Integer> unidades = new HashMap<>();
        Map<String, BigDecimal> montos = new HashMap<>();
        for (Venta v : ventasConDetalle(desde, hasta)) {
            if (v.getDetalles() == null) continue;
            for (DetalleVenta d : v.getDetalles()) {
                if (d.getMedicamento() == null) continue;
                String cat = "Sin categoría";
                try {
                    if (d.getMedicamento().getCategoria() != null
                            && d.getMedicamento().getCategoria().getNombre() != null) {
                        cat = d.getMedicamento().getCategoria().getNombre();
                    }
                } catch (Exception ignored) {
                    // lazy no inicializada
                }
                unidades.merge(cat, d.getCantidad() != null ? d.getCantidad() : 0, Integer::sum);
                montos.merge(cat, d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO, BigDecimal::add);
            }
        }
        return montos.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("categoria", e.getKey());
                    row.put("unidades", unidades.getOrDefault(e.getKey(), 0));
                    row.put("monto", e.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    private Map<Long, int[]> rankingUnidadesYMonto(LocalDate desde, LocalDate hasta) {
        // int[0]=unidades; usamos maps paralelos en el caller
        return Collections.emptyMap();
    }

    private void acumularVentasProducto(LocalDate desde, LocalDate hasta,
                                        Map<Long, Integer> unidades,
                                        Map<Long, BigDecimal> montos,
                                        Map<Long, String> nombres) {
        for (Venta v : ventasConDetalle(desde, hasta)) {
            if (v.getDetalles() == null) continue;
            for (DetalleVenta d : v.getDetalles()) {
                if (d.getMedicamento() == null) continue;
                Long id = d.getMedicamento().getId();
                unidades.merge(id, d.getCantidad() != null ? d.getCantidad() : 0, Integer::sum);
                montos.merge(id, d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO, BigDecimal::add);
                nombres.putIfAbsent(id, d.getMedicamento().getNombreComercial());
            }
        }
    }

    @Override
    public List<Map<String, Object>> medicamentosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {
        Map<Long, Integer> unidades = new HashMap<>();
        Map<Long, BigDecimal> montos = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();
        acumularVentasProducto(desde, hasta, unidades, montos, nombres);
        return unidades.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limite > 0 ? limite : 10)
                .map(e -> filaProducto(e.getKey(), nombres.get(e.getKey()), e.getValue(),
                        montos.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> medicamentosMenosVendidos(LocalDate desde, LocalDate hasta, int limite) {
        Map<Long, Integer> unidades = new HashMap<>();
        Map<Long, BigDecimal> montos = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();
        acumularVentasProducto(desde, hasta, unidades, montos, nombres);

        // Incluir activos sin ventas en el periodo con 0 unidades
        for (Medicamento m : medicamentoRepository.findByActivoTrue(
                org.springframework.data.domain.Pageable.unpaged()).getContent()) {
            nombres.putIfAbsent(m.getId(), m.getNombreComercial());
            unidades.putIfAbsent(m.getId(), 0);
            montos.putIfAbsent(m.getId(), BigDecimal.ZERO);
        }

        return unidades.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .limit(limite > 0 ? limite : 10)
                .map(e -> filaProducto(e.getKey(), nombres.get(e.getKey()), e.getValue(),
                        montos.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    private Map<String, Object> filaProducto(Long id, String nombre, int unidades, BigDecimal monto) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("medicamentoId", id);
        row.put("nombre", nombre);
        row.put("unidades", unidades);
        row.put("monto", monto);
        return row;
    }

    @Override
    public List<Map<String, Object>> clientesFrecuentes(LocalDate desde, LocalDate hasta, int limite) {
        Map<Long, Integer> frec = new HashMap<>();
        Map<Long, BigDecimal> montos = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();
        for (Venta ref : ventaRepository.findVentasPagadasEntre(inicio(desde), fin(hasta))) {
            Venta v = ventaRepository.findDetalladaById(ref.getId()).orElse(ref);
            if (v.getCliente() == null) continue;
            Long id = v.getCliente().getId();
            frec.merge(id, 1, Integer::sum);
            montos.merge(id, v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO, BigDecimal::add);
            nombres.putIfAbsent(id, v.getCliente().getNombreCompleto());
        }
        return frec.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limite > 0 ? limite : 10)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("clienteId", e.getKey());
                    row.put("nombre", nombres.get(e.getKey()));
                    row.put("compras", e.getValue());
                    row.put("monto", montos.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return row;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> empleadosMayoresVentas(LocalDate desde, LocalDate hasta, int limite) {
        Map<Long, Integer> conteos = new HashMap<>();
        Map<Long, BigDecimal> montos = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();
        for (Venta ref : ventaRepository.findVentasPagadasEntre(inicio(desde), fin(hasta))) {
            Venta v = ventaRepository.findDetalladaById(ref.getId()).orElse(ref);
            if (v.getEmpleado() == null) continue;
            Long id = v.getEmpleado().getId();
            conteos.merge(id, 1, Integer::sum);
            montos.merge(id, v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO, BigDecimal::add);
            nombres.putIfAbsent(id, v.getEmpleado().getNombreCompleto());
        }
        return montos.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limite > 0 ? limite : 10)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("empleadoId", e.getKey());
                    row.put("nombre", nombres.get(e.getKey()));
                    row.put("ventas", conteos.getOrDefault(e.getKey(), 0));
                    row.put("monto", e.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapInventario(Inventario i, String estado) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("medicamentoId", i.getMedicamento() != null ? i.getMedicamento().getId() : null);
        row.put("nombre", i.getMedicamento() != null ? i.getMedicamento().getNombreComercial() : null);
        row.put("disponible", i.getCantidadDisponible());
        row.put("stockMinimo", i.getStockMinimo());
        row.put("estado", estado);
        if (i.getMedicamento() != null && i.getMedicamento().getProveedor() != null) {
            row.put("proveedor", i.getMedicamento().getProveedor().getRazonSocial());
        }
        return row;
    }

    @Override
    public List<Map<String, Object>> stockBajo() {
        return inventarioRepository.findConStockBajo().stream()
                .filter(i -> !i.agotado())
                .map(i -> mapInventario(i, "STOCK_BAJO"))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> productosAgotados() {
        return inventarioRepository.findAgotados().stream()
                .map(i -> mapInventario(i, "AGOTADO"))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> stockBajoYAgotado() {
        List<Map<String, Object>> out = new ArrayList<>();
        out.addAll(stockBajo());
        out.addAll(productosAgotados());
        return out;
    }

    private Map<String, Object> mapLote(Lote l, String estado) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("loteId", l.getId());
        row.put("numeroLote", l.getNumeroLote());
        row.put("medicamento", l.getMedicamento() != null ? l.getMedicamento().getNombreComercial() : null);
        row.put("fechaVencimiento", l.getFechaVencimiento() != null ? l.getFechaVencimiento().toString() : null);
        row.put("cantidad", l.getCantidadDisponible());
        row.put("estado", estado);
        if (l.getMedicamento() != null && l.getMedicamento().getProveedor() != null) {
            row.put("proveedor", l.getMedicamento().getProveedor().getRazonSocial());
        }
        return row;
    }

    @Override
    public List<Map<String, Object>> proximosAVencer(Integer diasAnticipacion) {
        int dias = diasAnticipacion != null && diasAnticipacion > 0 ? diasAnticipacion : 30;
        LocalDate limite = LocalDate.now().plusDays(dias);
        LocalDate hoy = LocalDate.now();
        return loteRepository.findProximosAVencer(limite).stream()
                .filter(l -> l.getFechaVencimiento() != null && !l.getFechaVencimiento().isBefore(hoy))
                .map(l -> mapLote(l, "PROXIMO"))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> medicamentosVencidos() {
        return loteRepository.findVencidosNoActualizados().stream()
                .map(l -> mapLote(l, "VENCIDO"))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> vencimientos(Integer diasAnticipacion) {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();
        for (Map<String, Object> r : proximosAVencer(diasAnticipacion)) {
            out.add(r);
            if (r.get("loteId") != null) vistos.add(((Number) r.get("loteId")).longValue());
        }
        for (Map<String, Object> r : medicamentosVencidos()) {
            Long id = r.get("loteId") != null ? ((Number) r.get("loteId")).longValue() : null;
            if (id == null || vistos.add(id)) out.add(r);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> inventarioValorizado() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Inventario i : inventarioRepository.findAllConMedicamento()) {
            Medicamento m = i.getMedicamento();
            if (m == null) continue;
            BigDecimal pv = m.getPrecioVenta() != null ? m.getPrecioVenta() : BigDecimal.ZERO;
            BigDecimal pc = m.getPrecioCompra() != null ? m.getPrecioCompra() : BigDecimal.ZERO;
            int cant = i.getCantidadDisponible() != null ? i.getCantidadDisponible() : 0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("medicamentoId", m.getId());
            row.put("nombre", m.getNombreComercial());
            row.put("disponible", cant);
            row.put("precioVenta", pv);
            row.put("precioCompra", pc);
            row.put("valorVenta", pv.multiply(BigDecimal.valueOf(cant)));
            row.put("valorCosto", pc.multiply(BigDecimal.valueOf(cant)));
            if (m.getProveedor() != null) {
                row.put("proveedor", m.getProveedor().getRazonSocial());
            }
            if (m.getCategoria() != null) {
                row.put("categoria", m.getCategoria().getNombre());
            }
            out.add(row);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> movimientosInventario(LocalDate desde, LocalDate hasta) {
        List<MovimientoInventario> lista =
                movimientoInventarioRepository.findByFechaBetweenOrderByFechaDesc(inicio(desde), fin(hasta));
        List<Map<String, Object>> out = new ArrayList<>();
        for (MovimientoInventario mov : lista) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", mov.getId());
            row.put("fecha", mov.getFecha() != null ? mov.getFecha().toString() : null);
            row.put("tipo", mov.getTipoMovimiento() != null ? mov.getTipoMovimiento().name() : null);
            row.put("medicamento", mov.getMedicamento() != null ? mov.getMedicamento().getNombreComercial() : null);
            row.put("cantidad", mov.getCantidad());
            row.put("existenciaAnterior", mov.getExistenciaAnterior());
            row.put("nuevaExistencia", mov.getNuevaExistencia());
            row.put("motivo", mov.getMotivo());
            row.put("responsable", mov.getUsuarioResponsable() != null
                    ? mov.getUsuarioResponsable().getNombreCompleto() : null);
            out.add(row);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> comprasPorProveedor(LocalDate desde, LocalDate hasta) {
        Map<Long, BigDecimal> montos = new HashMap<>();
        Map<Long, Integer> conteos = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();
        for (Compra c : compraRepository.findByFechaCompraBetween(inicio(desde), fin(hasta))) {
            if (c.getProveedor() == null) continue;
            Long id = c.getProveedor().getId();
            BigDecimal total = c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO;
            montos.merge(id, total, BigDecimal::add);
            conteos.merge(id, 1, Integer::sum);
            nombres.putIfAbsent(id, c.getProveedor().getRazonSocial());
        }
        return montos.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("proveedorId", e.getKey());
                    row.put("nombre", nombres.get(e.getKey()));
                    row.put("compras", conteos.getOrDefault(e.getKey(), 0));
                    row.put("total", e.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> gananciasPeriodo(LocalDate desde, LocalDate hasta) {
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;
        for (Venta v : ventasConDetalle(desde, hasta)) {
            if (v.getTotal() != null) ingresos = ingresos.add(v.getTotal());
            if (v.getDetalles() != null) {
                for (DetalleVenta d : v.getDetalles()) {
                    if (d.getMedicamento() != null && d.getMedicamento().getPrecioCompra() != null
                            && d.getCantidad() != null) {
                        costo = costo.add(d.getMedicamento().getPrecioCompra()
                                .multiply(BigDecimal.valueOf(d.getCantidad())));
                    }
                }
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("desde", desde.toString());
        m.put("hasta", hasta.toString());
        m.put("ingresos", ingresos);
        m.put("costoEstimado", costo);
        m.put("ganancia", ingresos.subtract(costo));
        return m;
    }

    @Override
    public List<Map<String, Object>> formulasRegistradas(LocalDate desde, LocalDate hasta) {
        return formulaMedicaRepository.findAll().stream()
                .filter(f -> f.getFechaExpedicion() != null
                        && !f.getFechaExpedicion().isBefore(desde)
                        && !f.getFechaExpedicion().isAfter(hasta))
                .map(f -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", f.getId());
                    row.put("fechaExpedicion", f.getFechaExpedicion().toString());
                    row.put("cliente", f.getCliente() != null ? f.getCliente().getNombreCompleto() : null);
                    row.put("medico", f.getNombreMedico());
                    row.put("entidadSalud", f.getEntidadSalud());
                    row.put("duracionDias", f.getDuracionTratamientoDias());
                    return row;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> catalogoReportes() {
        List<Map<String, Object>> cat = new ArrayList<>();
        cat.add(meta("ventas-diarias", "Ventas diarias", "ventas", "Resumen y detalle del día"));
        cat.add(meta("ventas-semanales", "Ventas semanales", "ventas", "Últimos 7 días"));
        cat.add(meta("ventas-mensuales", "Ventas mensuales", "ventas", "Mes en curso"));
        cat.add(meta("ventas-detalle", "Detalle de ventas", "ventas", "Listado del periodo filtrado"));
        cat.add(meta("ventas-metodo-pago", "Ventas por método de pago", "ventas", null));
        cat.add(meta("ventas-categoria", "Ventas por categoría", "ventas", null));
        cat.add(meta("mas-vendidos", "Medicamentos más vendidos", "productos", null));
        cat.add(meta("menos-vendidos", "Productos menos vendidos", "productos", null));
        cat.add(meta("clientes-frecuentes", "Clientes frecuentes", "personas", null));
        cat.add(meta("empleados-ventas", "Empleados con mayores ventas", "personas", null));
        cat.add(meta("stock-bajo", "Productos con stock bajo", "inventario", null));
        cat.add(meta("agotados", "Productos agotados", "inventario", null));
        cat.add(meta("proximos-vencer", "Medicamentos próximos a vencer", "inventario", null));
        cat.add(meta("vencidos", "Medicamentos vencidos", "inventario", null));
        cat.add(meta("inventario-valorizado", "Inventario valorizado", "inventario", null));
        cat.add(meta("movimientos", "Movimientos de inventario", "inventario", null));
        cat.add(meta("compras-proveedor", "Compras por proveedor", "compras",
                "Proveedores asociados a medicamentos"));
        cat.add(meta("ganancias", "Ganancias por periodo", "finanzas", null));
        cat.add(meta("formulas", "Fórmulas médicas registradas", "formulas", null));
        return cat;
    }

    private Map<String, Object> meta(String id, String nombre, String grupo, String nota) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nombre", nombre);
        m.put("grupo", grupo);
        if (nota != null) m.put("nota", nota);
        return m;
    }
}
