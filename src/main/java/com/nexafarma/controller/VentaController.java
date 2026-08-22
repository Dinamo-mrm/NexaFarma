package com.nexafarma.controller;

import com.nexafarma.entity.EstadoVenta;
import com.nexafarma.entity.Venta;
import com.nexafarma.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping
    public ResponseEntity<Venta> crear(@Valid @RequestBody Venta venta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crear(venta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Venta> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<Venta>> listar(@RequestParam(required = false) EstadoVenta estado, Pageable pageable) {
        Page<Venta> resultado = (estado == null) ? ventaService.listar(pageable) : ventaService.listarPorEstado(estado, pageable);
        return ResponseEntity.ok(resultado);
    }

    @PatchMapping("/{id}/anular")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        ventaService.anular(id);
        return ResponseEntity.noContent().build();
    }
}
