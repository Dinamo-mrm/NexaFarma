package com.nexafarma.controller;

import com.nexafarma.entity.DetalleVenta;
import com.nexafarma.entity.Venta;
import com.nexafarma.service.VentaService;
import com.nexafarma.util.SimplePdfWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Controller
public class FacturaController {

    private final VentaService ventaService;

    public FacturaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    /** Vista imprimible HTML (también se puede Guardar como PDF desde el navegador). */
    @GetMapping(value = "/ventas/{id}/factura", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String facturaHtml(@PathVariable Long id) {
        Venta v = ventaService.obtenerPorId(id);
        return buildHtml(v);
    }

    /** PDF nativo generado en servidor (sin dependencias externas). */
    @GetMapping(value = "/ventas/{id}/factura.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> facturaPdf(@PathVariable Long id) {
        Venta v = ventaService.obtenerPorId(id);
        byte[] pdf = buildPdf(v);
        String filename = "factura-" + (v.getNumeroVenta() != null ? v.getNumeroVenta() : id) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private byte[] buildPdf(Venta v) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        SimplePdfWriter w = new SimplePdfWriter(11);
        w.line("NexaFarma - Comprobante de venta")
                .line("================================")
                .line("No. " + nullSafe(v.getNumeroVenta()))
                .line("Fecha: " + (v.getFecha() != null ? v.getFecha().format(fmt) : "-"))
                .blank();
        if (v.getCliente() != null) {
            w.line("Cliente: " + nullSafe(v.getCliente().getNombreCompleto()))
                    .line("Documento: " + nullSafe(v.getCliente().getDocumento()));
        }
        if (v.getEmpleado() != null) {
            w.line("Atendido por: " + nullSafe(v.getEmpleado().getNombreCompleto()));
        }
        w.blank().line("Detalle")
                .line("--------------------------------");
        if (v.getDetalles() != null) {
            for (DetalleVenta d : v.getDetalles()) {
                String nombre = d.getMedicamento() != null ? d.getMedicamento().getNombreComercial() : "-";
                String lote = d.getLote() != null ? d.getLote().getNumeroLote() : "-";
                w.line(nombre + " | Lote " + lote + " | x" + d.getCantidad()
                        + " | " + money(d.getPrecioUnitario()) + " | " + money(d.getSubtotal()));
            }
        }
        w.line("--------------------------------")
                .line("Subtotal:  " + money(v.getSubtotal()))
                .line("Descuento: " + money(v.getDescuento()))
                .line("Impuestos: " + money(v.getImpuestos()))
                .line("TOTAL:     " + money(v.getTotal()))
                .line("Pago: " + (v.getMetodoPago() != null ? v.getMetodoPago().name() : "-"))
                .blank()
                .line("Gracias por su compra.");
        return w.build();
    }

    private String buildHtml(Venta v) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <!DOCTYPE html><html lang="es"><head><meta charset="utf-8"/>
            <title>Factura %s</title>
            <style>
              body{font-family:system-ui,sans-serif;max-width:720px;margin:24px auto;color:#111}
              h1{font-size:1.25rem;margin:0} .muted{color:#666;font-size:.9rem}
              table{width:100%%;border-collapse:collapse;margin-top:16px}
              th,td{border-bottom:1px solid #ddd;padding:8px;text-align:left}
              th{background:#f5f5f5} .right{text-align:right}
              .totales{margin-top:16px;float:right;min-width:220px}
              .totales div{display:flex;justify-content:space-between;margin:4px 0}
              .total{font-weight:700;font-size:1.1rem;border-top:2px solid #111;padding-top:6px}
              .actions{margin-bottom:16px;display:flex;gap:8px}
              @media print{.no-print{display:none}}
            </style></head><body>
            """.formatted(esc(v.getNumeroVenta())));
        sb.append("<div class='actions no-print'>");
        sb.append("<button onclick='window.print()'>Imprimir</button>");
        sb.append("<a href='/ventas/").append(v.getId()).append("/factura.pdf'>Descargar PDF</a>");
        sb.append("</div>");
        sb.append("<h1>NexaFarma — Comprobante de venta</h1>");
        sb.append("<p class='muted'>N.º ").append(esc(v.getNumeroVenta()));
        if (v.getFecha() != null) sb.append(" · ").append(v.getFecha().format(fmt));
        sb.append("</p>");
        if (v.getCliente() != null) {
            sb.append("<p><strong>Cliente:</strong> ").append(esc(v.getCliente().getNombreCompleto()));
            sb.append(" · Doc: ").append(esc(v.getCliente().getDocumento())).append("</p>");
        }
        if (v.getEmpleado() != null) {
            sb.append("<p class='muted'>Atendido por: ").append(esc(v.getEmpleado().getNombreCompleto())).append("</p>");
        }
        sb.append("<table><thead><tr><th>Producto</th><th>Lote</th><th class='right'>Cant.</th><th class='right'>P.unit</th><th class='right'>Subtotal</th></tr></thead><tbody>");
        if (v.getDetalles() != null) {
            for (DetalleVenta d : v.getDetalles()) {
                String nombre = d.getMedicamento() != null ? d.getMedicamento().getNombreComercial() : "—";
                String lote = d.getLote() != null ? d.getLote().getNumeroLote() : "—";
                sb.append("<tr><td>").append(esc(nombre)).append("</td><td>").append(esc(lote))
                        .append("</td><td class='right'>").append(d.getCantidad())
                        .append("</td><td class='right'>").append(money(d.getPrecioUnitario()))
                        .append("</td><td class='right'>").append(money(d.getSubtotal())).append("</td></tr>");
            }
        }
        sb.append("</tbody></table><div class='totales'>");
        sb.append("<div><span>Subtotal</span><span>").append(money(v.getSubtotal())).append("</span></div>");
        sb.append("<div><span>Descuento</span><span>").append(money(v.getDescuento())).append("</span></div>");
        sb.append("<div><span>Impuestos</span><span>").append(money(v.getImpuestos())).append("</span></div>");
        sb.append("<div class='total'><span>Total</span><span>").append(money(v.getTotal())).append("</span></div>");
        sb.append("<div class='muted'>Pago: ").append(v.getMetodoPago() != null ? v.getMetodoPago().name() : "—").append("</div>");
        sb.append("</div></body></html>");
        return sb.toString();
    }

    private static String money(BigDecimal v) {
        if (v == null) return "$0";
        return "$" + String.format("%,.2f", v);
    }

    private static String nullSafe(String s) {
        return s == null ? "-" : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
