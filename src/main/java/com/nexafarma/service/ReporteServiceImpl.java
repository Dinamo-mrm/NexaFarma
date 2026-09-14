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

    public ReporteServiceImpl(VentaRepository ventaRepository,
                              InventarioRepository inventarioRepository,
                              LoteRepository loteRepository,
                              CompraRepository compraRepository,
                              FormulaMedicaRepository formulaMedicaRepository) {
        this.ventaRepository = ventaRepository;
        this.inventarioRepository = inventarioRepository;
        this.loteRepository = loteRepository;
        this.compraRepository = compraRepository;
        this.formulaMedicaRepository = formulaMedicaRepository;
    }

    private LocalDateTime inicio(LocalDate d) {
        return d.atStartOfDay();
    }

    private LocalDateTime fin(LocalDate d) {
        return d.atTime(LocalTime.MAX);
    }

    /** Carga ventas del rango con cliente, empleado y detalles (evita LazyInitialization). */
    private List<Venta> ventasConDetalle(LocalDate desde, LocalDate hasta) {
        List<Venta> base = ventaRepository.findVentasPagadasEntre(inicio(desde), fin(hasta));
        List<Venta> out = new ArrayList<>();
        for (Venta v : base) {
            ventaRepository.findDetalladaById(v.getId()).ifPresent(out::add);
        }
        return out;
    }

    @Override
    public Map<String, Object> resumenVentas(LocalDate desde, LocalDate hasta) {
        List<Venta> ventas = ventaRepository.findVentasPagadasEntre(inicio(desde), fin(hasta));
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
    public List<Map<String, Object>> ventasDetalle(LocalDate desde, LocalDate hasta) {
        List<Venta> ventas = ventaRepository.findVentasPagadasEntre(inicio(desde), fin(hasta));
        // Cliente/empleado pueden ser LAZY: usamos findDetallada solo si hace falta;
        // el EntityGraph de otras consultas no aplica aquí, así que recargamos.
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Venta ref : ventas) {
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
    public List<Map<String, Object>> medicamentosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {
        Map<Long, Integer> unidades = new HashMap<>();
        Map<Long, BigDecimal> montos = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();

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

        return unidades.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limite > 0 ? limite : 10)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("medicamentoId", e.getKey());
                    row.put("nombre", nombres.get(e.getKey()));
                    row.put("unidades", e.getValue());
                    row.put("monto", montos.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return row;
                })
                .collect(Collectors.toList());
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

    @Override
    public List<Map<String, Object>> stockBajoYAgotado() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Inventario i : inventarioRepository.findConStockBajo()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("medicamentoId", i.getMedicamento() != null ? i.getMedicamento().getId() : null);
            row.put("nombre", i.getMedicamento() != null ? i.getMedicamento().getNombreComercial() : null);
            row.put("disponible", i.getCantidadDisponible());
            row.put("stockMinimo", i.getStockMinimo());
            row.put("estado", i.agotado() ? "AGOTADO" : "STOCK_BAJO");
            out.add(row);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> vencimientos(Integer diasAnticipacion) {
        int dias = diasAnticipacion != null && diasAnticipacion > 0 ? diasAnticipacion : 30;
        LocalDate limite = LocalDate.now().plusDays(dias);
        List<Map<String, Object>> out = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();

        for (Lote l : loteRepository.findProximosAVencer(limite)) {
            String estado = (l.getFechaVencimiento() != null && l.getFechaVencimiento().isBefore(LocalDate.now()))
                    ? "VENCIDO" : "PROXIMO";
            out.add(mapLote(l, estado));
            vistos.add(l.getId());
        }
        for (Lote l : loteRepository.findVencidosNoActualizados()) {
            if (vistos.add(l.getId())) {
                out.add(mapLote(l, "VENCIDO"));
            }
        }
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
        return row;
    }

    @Override
    public List<Map<String, Object>> inventarioValorizado() {
        List<Map<String, Object>> out = new ArrayList<>();
        // findConStockBajo ya hace JOIN FETCH; para todos usamos findAll y aceptamos N+1 acotado
        for (Inventario i : inventarioRepository.findAll()) {
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
            if (v.getTotal() != null) {
                ingresos = ingresos.add(v.getTotal());
            }
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
}
