package com.nexafarma.controller;

import com.nexafarma.dto.VentaRequest;
import com.nexafarma.dto.VentaResponse;
import com.nexafarma.entity.EstadoVenta;
import com.nexafarma.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API REST del punto de venta. Responsabilidad del Desarrollador 3.
 * TODO(Dev1): asegurar estos endpoints con @PreAuthorize (VENDEDOR / FARMACEUTICO / ADMINISTRADOR).
 */
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    public ResponseEntity<VentaResponse> crear(@Valid @RequestBody VentaRequest request) {
        VentaResponse creada = ventaService.crearVenta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<VentaResponse> anular(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.anularVenta(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<VentaResponse>> listarPorEstado(
            @RequestParam(defaultValue = "PAGADA") EstadoVenta estado,
            Pageable pageable) {
        return ResponseEntity.ok(ventaService.listarPorEstado(estado, pageable));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<VentaResponse>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(ventaService.listarPorCliente(clienteId));
    }

    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<List<VentaResponse>> listarPorEmpleadoYRango(
            @PathVariable Long empleadoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(ventaService.listarPorEmpleadoYRango(empleadoId, desde, hasta));
    }
}
