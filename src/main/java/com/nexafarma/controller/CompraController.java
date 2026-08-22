package com.nexafarma.controller;

import com.nexafarma.entity.Compra;
import com.nexafarma.entity.EstadoCompra;
import com.nexafarma.service.CompraService;
import com.nexafarma.service.CompraService.DatosLoteRecepcion;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    public ResponseEntity<Compra> crear(@Valid @RequestBody Compra compra) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.crear(compra));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Compra> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<Compra>> listar(@RequestParam(required = false) EstadoCompra estado, Pageable pageable) {
        Page<Compra> resultado = (estado == null) ? compraService.listar(pageable) : compraService.listarPorEstado(estado, pageable);
        return ResponseEntity.ok(resultado);
    }

    /** Body: { "<detalleId>": {"numeroLote": "...", "fechaFabricacion": "yyyy-MM-dd", "fechaVencimiento": "yyyy-MM-dd"} } */
    @PatchMapping("/{id}/recibir")
    public ResponseEntity<Compra> recibir(@PathVariable Long id, @RequestBody Map<Long, DatosLoteRecepcion> datosLotePorDetalle) {
        return ResponseEntity.ok(compraService.recibir(id, datosLotePorDetalle));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        compraService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
