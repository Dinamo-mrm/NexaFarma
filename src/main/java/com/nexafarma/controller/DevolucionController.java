package com.nexafarma.controller;

import com.nexafarma.entity.Devolucion;
import com.nexafarma.entity.TipoDevolucion;
import com.nexafarma.service.DevolucionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devoluciones")
public class DevolucionController {

    private final DevolucionService devolucionService;

    public DevolucionController(DevolucionService devolucionService) {
        this.devolucionService = devolucionService;
    }

    @PostMapping
    public ResponseEntity<Devolucion> crear(@Valid @RequestBody Devolucion devolucion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(devolucionService.crear(devolucion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Devolucion> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(devolucionService.obtenerPorId(id));
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<Devolucion>> listarPendientes() {
        return ResponseEntity.ok(devolucionService.listarPendientesDeValidacion());
    }

    @GetMapping
    public ResponseEntity<List<Devolucion>> listarPorTipo(@RequestParam TipoDevolucion tipo) {
        return ResponseEntity.ok(devolucionService.listarPorTipo(tipo));
    }

    @PatchMapping("/{id}/validar")
    public ResponseEntity<Devolucion> validar(@PathVariable Long id, @RequestParam Long farmaceuticoId) {
        return ResponseEntity.ok(devolucionService.validar(id, farmaceuticoId));
    }
}
