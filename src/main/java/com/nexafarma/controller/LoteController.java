package com.nexafarma.controller;

import com.nexafarma.entity.Lote;
import com.nexafarma.service.LoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    @PostMapping
    public ResponseEntity<Lote> crear(@RequestBody Lote lote) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteService.crear(lote));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lote> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(loteService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<Lote>> listarPorMedicamento(@RequestParam Long medicamentoId) {
        return ResponseEntity.ok(loteService.listarPorMedicamento(medicamentoId));
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<Lote>> listarVencidos() {
        return ResponseEntity.ok(loteService.listarVencidos());
    }

    @GetMapping("/proximos-a-vencer")
    public ResponseEntity<List<Lote>> listarProximosAVencer(@RequestParam(required = false) Integer dias) {
        return ResponseEntity.ok(loteService.listarProximosAVencer(dias));
    }

    @GetMapping("/cuarentena")
    public ResponseEntity<List<Lote>> listarEnCuarentena() {
        return ResponseEntity.ok(loteService.listarEnCuarentena());
    }

    @PatchMapping("/{id}/liberar-cuarentena")
    public ResponseEntity<Lote> liberarCuarentena(
            @PathVariable Long id,
            @RequestParam(required = false) Long empleadoRegenteId,
            @RequestBody(required = false) Map<String, Long> body) {
        Long regenteId = empleadoRegenteId;
        if (regenteId == null && body != null) {
            regenteId = body.get("empleadoRegenteId");
        }
        return ResponseEntity.ok(loteService.liberarCuarentena(id, regenteId));
    }
}
