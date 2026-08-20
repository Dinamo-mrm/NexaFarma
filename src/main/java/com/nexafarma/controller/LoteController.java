package com.nexafarma.controller;

import com.nexafarma.entity.Lote;
import com.nexafarma.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    @PostMapping
    public ResponseEntity<Lote> crear(@Valid @RequestBody Lote lote) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loteService.crear(lote));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lote> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(loteService.obtenerPorId(id));
    }

    @GetMapping("/medicamento/{medicamentoId}")
    public ResponseEntity<List<Lote>> listarPorMedicamento(
            @PathVariable Long medicamentoId,
            @RequestParam(defaultValue = "false") boolean fefo) {
        List<Lote> lotes = fefo
                ? loteService.listarActivosFefo(medicamentoId)
                : loteService.listarPorMedicamento(medicamentoId);
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<Lote>> listarVencidos() {
        return ResponseEntity.ok(loteService.listarVencidos());
    }

    @GetMapping("/proximos-a-vencer")
    public ResponseEntity<List<Lote>> listarProximosAVencer(@RequestParam(required = false) Integer dias) {
        return ResponseEntity.ok(loteService.listarProximosAVencer(dias));
    }
}