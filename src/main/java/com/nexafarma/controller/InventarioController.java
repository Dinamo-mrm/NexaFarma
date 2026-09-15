package com.nexafarma.controller;

import com.nexafarma.entity.Inventario;
import com.nexafarma.service.InventarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/medicamento/{medicamentoId}")
    public ResponseEntity<Inventario> obtenerPorMedicamento(@PathVariable Long medicamentoId) {
        return ResponseEntity.ok(inventarioService.obtenerPorMedicamento(medicamentoId));
    }

    @GetMapping
    public ResponseEntity<List<Inventario>> listarTodos() {
        return ResponseEntity.ok(inventarioService.listarTodos());
    }

    @GetMapping("/stock-bajo")
    public ResponseEntity<List<Inventario>> listarStockBajo() {
        return ResponseEntity.ok(inventarioService.listarStockBajo());
    }

    @GetMapping("/agotados")
    public ResponseEntity<List<Inventario>> listarAgotados() {
        return ResponseEntity.ok(inventarioService.listarAgotados());
    }
}