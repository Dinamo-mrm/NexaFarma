package com.nexafarma.controller;

import com.nexafarma.dto.CompraRequest;
import com.nexafarma.dto.CompraResponse;
import com.nexafarma.dto.RecibirCompraRequest;
import com.nexafarma.entity.EstadoCompra;
import com.nexafarma.service.CompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST de Compras a proveedores. Responsabilidad del Desarrollador 3.
 * TODO(Dev1): asegurar estos endpoints con @PreAuthorize (ADMINISTRADOR / FARMACEUTICO).
 */
@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @PostMapping
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody CompraRequest request) {
        CompraResponse creada = compraService.registrarCompra(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PostMapping("/{id}/recibir")
    public ResponseEntity<CompraResponse> recibir(@PathVariable Long id,
                                                    @Valid @RequestBody RecibirCompraRequest request) {
        return ResponseEntity.ok(compraService.recibirCompra(id, request));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<CompraResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.cancelarCompra(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<CompraResponse>> listarPorEstado(
            @RequestParam(defaultValue = "PENDIENTE") EstadoCompra estado,
            Pageable pageable) {
        return ResponseEntity.ok(compraService.listarPorEstado(estado, pageable));
    }

    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<CompraResponse>> listarPorProveedor(@PathVariable Long proveedorId) {
        return ResponseEntity.ok(compraService.listarPorProveedor(proveedorId));
    }
}
