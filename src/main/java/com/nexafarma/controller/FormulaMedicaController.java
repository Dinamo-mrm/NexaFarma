package com.nexafarma.controller;

import com.nexafarma.dto.FormulaMedicaRequest;
import com.nexafarma.dto.FormulaMedicaResponse;
import com.nexafarma.service.FormulaMedicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST de Fórmulas Médicas. Responsabilidad del Desarrollador 3.
 * TODO(Dev1): asegurar estos endpoints con @PreAuthorize (FARMACEUTICO / ADMINISTRADOR).
 */
@RestController
@RequestMapping("/api/formulas-medicas")
@RequiredArgsConstructor
public class FormulaMedicaController {

    private final FormulaMedicaService formulaMedicaService;

    @PostMapping
    public ResponseEntity<FormulaMedicaResponse> registrar(@Valid @RequestBody FormulaMedicaRequest request) {
        FormulaMedicaResponse creada = formulaMedicaService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormulaMedicaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(formulaMedicaService.obtenerPorId(id));
    }

    @GetMapping("/{id}/vigencia")
    public ResponseEntity<Map<String, Boolean>> verificarVigencia(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("vigente", formulaMedicaService.esVigente(id)));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<FormulaMedicaResponse>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(formulaMedicaService.listarPorCliente(clienteId));
    }
}
