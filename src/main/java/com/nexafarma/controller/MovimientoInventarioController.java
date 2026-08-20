package com.nexafarma.controller;

import com.nexafarma.entity.MovimientoInventario;
import com.nexafarma.service.MovimientoInventarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movimientos-inventario")
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoService;

    public MovimientoInventarioController(MovimientoInventarioService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @PostMapping("/entrada")
    public ResponseEntity<MovimientoInventario> registrarEntrada(@RequestBody MovimientoEntradaSalidaRequest request) {
        MovimientoInventario movimiento = movimientoService.registrarEntrada(
                request.loteId(), request.cantidad(), request.empleadoResponsableId(), request.motivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(movimiento);
    }

    @PostMapping("/salida")
    public ResponseEntity<MovimientoInventario> registrarSalida(@RequestBody MovimientoEntradaSalidaRequest request) {
        MovimientoInventario movimiento = movimientoService.registrarSalida(
                request.loteId(), request.cantidad(), request.empleadoResponsableId(), request.motivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(movimiento);
    }

    @GetMapping("/medicamento/{medicamentoId}")
    public ResponseEntity<Page<MovimientoInventario>> listarPorMedicamento(
            @PathVariable Long medicamentoId, Pageable pageable) {
        return ResponseEntity.ok(movimientoService.listarPorMedicamento(medicamentoId, pageable));
    }

    /** Record simple de request; el proyecto no usa DTOs, pero esto es indispensable para no exponer la entidad Lote/Empleado completa en el body de entrada. */
    public record MovimientoEntradaSalidaRequest(Long loteId, Integer cantidad, Long empleadoResponsableId, String motivo) {}
}