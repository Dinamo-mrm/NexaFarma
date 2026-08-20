package com.nexafarma.controller;

import com.nexafarma.entity.Medicamento;
import com.nexafarma.service.MedicamentoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medicamentos")
public class MedicamentoController {

    private final MedicamentoService medicamentoService;

    public MedicamentoController(MedicamentoService medicamentoService) {
        this.medicamentoService = medicamentoService;
    }

    @PostMapping
    public ResponseEntity<Medicamento> crear(@Valid @RequestBody Medicamento medicamento) {
        Medicamento creado = medicamentoService.crear(medicamento);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medicamento> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(medicamentoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<Medicamento>> listar(
            @RequestParam(required = false) String nombre,
            Pageable pageable) {
        Page<Medicamento> resultado = (nombre == null || nombre.isBlank())
                ? medicamentoService.listarActivos(pageable)
                : medicamentoService.buscarPorNombre(nombre, pageable);
        return ResponseEntity.ok(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medicamento> actualizar(@PathVariable Long id, @Valid @RequestBody Medicamento medicamento) {
        return ResponseEntity.ok(medicamentoService.actualizar(id, medicamento));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        medicamentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}