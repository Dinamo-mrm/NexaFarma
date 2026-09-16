package com.nexafarma.controller;

import com.nexafarma.entity.PrecioHistorial;
import com.nexafarma.repository.PrecioHistorialRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/precios/historial")
public class PrecioHistorialController {

    private final PrecioHistorialRepository repo;

    public PrecioHistorialController(PrecioHistorialRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<List<PrecioHistorial>> porProducto(@RequestParam Long productoId) {
        return ResponseEntity.ok(repo.findByProductoIdOrderByCreadoEnDesc(productoId));
    }
}
