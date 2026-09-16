package com.nexafarma.controller;

import com.nexafarma.entity.EstadoCompra;
import com.nexafarma.entity.EstadoDomicilio;
import com.nexafarma.repository.CompraRepository;
import com.nexafarma.repository.DetalleVentaRepository;
import com.nexafarma.repository.DomicilioRepository;
import com.nexafarma.service.AlertaService;
import com.nexafarma.service.CrmService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final CompraRepository compraRepository;
    private final DomicilioRepository domicilioRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final AlertaService alertaService;
    private final CrmService crmService;

    public DashboardApiController(CompraRepository compraRepository,
                                   DomicilioRepository domicilioRepository,
                                   DetalleVentaRepository detalleVentaRepository,
                                   AlertaService alertaService,
                                   CrmService crmService) {
        this.compraRepository = compraRepository;
        this.domicilioRepository = domicilioRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.alertaService = alertaService;
        this.crmService = crmService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> resumen() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("comprasPendientes", compraRepository.findByEstado(EstadoCompra.PENDIENTE, PageRequest.of(0, 10)).getContent());
        m.put("domiciliosPendientes", domicilioRepository.findByEstado(EstadoDomicilio.PENDIENTE));
        m.put("lotesVencidos", alertaService.obtenerLotesVencidos());
        m.put("stockBajo", alertaService.obtenerAlertasStockBajo());
        m.put("proximosVencer", alertaService.obtenerAlertasProximosAVencer(30));

        List<Map<String, Object>> top = new ArrayList<>();
        for (Object[] fila : detalleVentaRepository.findMedicamentosMasVendidos()) {
            if (top.size() >= 10) break;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("medicamentoId", fila[0]);
            row.put("nombre", fila.length > 2 ? fila[1] : null);
            row.put("unidades", fila.length > 2 ? fila[2] : fila[1]);
            top.add(row);
        }
        m.put("masVendidos", top);
        try {
            m.put("recomprasPendientes", crmService.listarRecomprasPendientes());
        } catch (Exception e) {
            m.put("recomprasPendientes", List.of());
        }
        return ResponseEntity.ok(m);
    }
}

