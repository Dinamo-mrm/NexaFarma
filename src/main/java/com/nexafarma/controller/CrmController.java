package com.nexafarma.controller;

import com.nexafarma.entity.AlertaRecompra;
import com.nexafarma.entity.Cliente;
import com.nexafarma.service.CrmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crm")
public class CrmController {

    private final CrmService crmService;

    public CrmController(CrmService crmService) {
        this.crmService = crmService;
    }

    @GetMapping("/recompras")
    public ResponseEntity<List<AlertaRecompra>> recompras() {
        return ResponseEntity.ok(crmService.listarRecomprasPendientes());
    }

    @PatchMapping("/recompras/{id}/contactado")
    public ResponseEntity<AlertaRecompra> contactado(@PathVariable Long id) {
        return ResponseEntity.ok(crmService.marcarContactado(id));
    }

    @PostMapping("/recompras/generar")
    public ResponseEntity<Map<String, Object>> generar() {
        return ResponseEntity.ok(Map.of("creadas", crmService.generarAlertasRecompra()));
    }

    @PostMapping("/puntos/acumular/{ventaId}")
    public ResponseEntity<Void> acumular(@PathVariable Long ventaId) {
        crmService.acumularPuntosPorVenta(ventaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/puntos/redimir")
    public ResponseEntity<Cliente> redimir(@RequestBody Map<String, Object> body) {
        Long clienteId = Long.valueOf(body.get("clienteId").toString());
        int puntos = Integer.parseInt(body.get("puntos").toString());
        return ResponseEntity.ok(crmService.redimirPuntos(clienteId, puntos));
    }
}
