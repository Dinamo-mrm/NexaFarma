package com.nexafarma.controller;

import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Lote;
import com.nexafarma.service.AlertaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping("/stock-bajo")
    public ResponseEntity<List<Inventario>> stockBajo() {
        return ResponseEntity.ok(alertaService.obtenerAlertasStockBajo());
    }

    @GetMapping("/proximos-vencimientos")
    public ResponseEntity<List<Lote>> proximosVencimientos(@RequestParam(required = false) Integer dias) {
        return ResponseEntity.ok(alertaService.obtenerAlertasProximosAVencer(dias));
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<Lote>> vencidos() {
        return ResponseEntity.ok(alertaService.obtenerLotesVencidos());
    }

    /** Ejecuta manualmente la revisión nocturna (admin). */
    @PostMapping("/revision-nocturna")
    public ResponseEntity<Map<String, Object>> revisionNocturna() {
        return ResponseEntity.ok(alertaService.ejecutarRevisionNocturna());
    }

    @GetMapping("/stock-minimo/sugerencias")
    public ResponseEntity<List<Map<String, Object>>> sugerenciasStockMinimo() {
        return ResponseEntity.ok(alertaService.sugerirStockMinimoDinamico());
    }

    @PostMapping("/stock-minimo/aplicar")
    public ResponseEntity<Map<String, Object>> aplicarStockMinimo() {
        int n = alertaService.aplicarSugerenciasStockMinimo();
        return ResponseEntity.ok(Map.of("aplicados", n));
    }
}
