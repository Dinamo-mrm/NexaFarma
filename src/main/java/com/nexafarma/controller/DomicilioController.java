package com.nexafarma.controller;

import com.nexafarma.entity.Domicilio;
import com.nexafarma.entity.EstadoDomicilio;
import com.nexafarma.service.DomicilioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/domicilios")
public class DomicilioController {

    private final DomicilioService domicilioService;

    public DomicilioController(DomicilioService domicilioService) {
        this.domicilioService = domicilioService;
    }

    @PostMapping
    public ResponseEntity<Domicilio> crear(@Valid @RequestBody Domicilio domicilio) {
        return ResponseEntity.status(HttpStatus.CREATED).body(domicilioService.crear(domicilio));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Domicilio> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(domicilioService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<Domicilio>> listarPorEstado(@RequestParam EstadoDomicilio estado) {
        return ResponseEntity.ok(domicilioService.listarPorEstado(estado));
    }

    @PatchMapping("/{id}/asignar")
    public ResponseEntity<Domicilio> asignarDomiciliario(@PathVariable Long id, @RequestParam Long domiciliarioId) {
        return ResponseEntity.ok(domicilioService.asignarDomiciliario(id, domiciliarioId));
    }

    @PatchMapping("/{id}/en-camino")
    public ResponseEntity<Domicilio> marcarEnCamino(@PathVariable Long id) {
        return ResponseEntity.ok(domicilioService.marcarEnCamino(id));
    }

    @PatchMapping("/{id}/entregado")
    public ResponseEntity<Domicilio> marcarEntregado(@PathVariable Long id) {
        return ResponseEntity.ok(domicilioService.marcarEntregado(id));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Domicilio> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(domicilioService.cancelar(id));
    }
}
