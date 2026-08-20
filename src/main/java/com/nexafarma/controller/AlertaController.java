package com.nexafarma.controller;

import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Lote;
import com.nexafarma.service.AlertaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}