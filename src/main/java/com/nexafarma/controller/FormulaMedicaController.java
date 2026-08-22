package com.nexafarma.controller;

import com.nexafarma.entity.FormulaMedica;
import com.nexafarma.service.FormulaMedicaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/formulas-medicas")
public class FormulaMedicaController {

    private final FormulaMedicaService formulaMedicaService;

    public FormulaMedicaController(FormulaMedicaService formulaMedicaService) {
        this.formulaMedicaService = formulaMedicaService;
    }

    @PostMapping
    public ResponseEntity<FormulaMedica> crear(@Valid @RequestBody FormulaMedica formula) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formulaMedicaService.crear(formula));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormulaMedica> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(formulaMedicaService.obtenerPorId(id));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<FormulaMedica>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(formulaMedicaService.listarPorCliente(clienteId));
    }
}
