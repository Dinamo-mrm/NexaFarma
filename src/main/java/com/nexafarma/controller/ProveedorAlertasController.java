package com.nexafarma.controller;

import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.repository.InventarioRepository;
import com.nexafarma.repository.MedicamentoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Resumen de catálogo y alertas de stock por proveedor
 * (el stock sigue viviendo en lotes/inventario, no en el proveedor).
 */
@RestController
@RequestMapping("/api/proveedores")
public class ProveedorAlertasController {

    private final MedicamentoRepository medicamentoRepository;
    private final InventarioRepository inventarioRepository;

    public ProveedorAlertasController(MedicamentoRepository medicamentoRepository,
                                       InventarioRepository inventarioRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.inventarioRepository = inventarioRepository;
    }

    @GetMapping("/{id}/resumen-stock")
    public ResponseEntity<Map<String, Object>> resumenStock(@PathVariable Long id) {
        List<Medicamento> meds = medicamentoRepository.findByProveedorIdAndActivoTrue(id);
        List<Inventario> todos = inventarioRepository.findAllConMedicamento();
        Map<Long, Inventario> porMed = new HashMap<>();
        for (Inventario inv : todos) {
            if (inv.getMedicamento() != null && inv.getMedicamento().getId() != null) {
                porMed.put(inv.getMedicamento().getId(), inv);
            }
        }

        int productos = meds.size();
        int stockBajo = 0;
        int sinStock = 0;
        List<Map<String, Object>> alertas = new ArrayList<>();
        for (Medicamento m : meds) {
            Inventario inv = porMed.get(m.getId());
            int disp = inv != null && inv.getCantidadDisponible() != null ? inv.getCantidadDisponible() : 0;
            int min = inv != null && inv.getStockMinimo() != null ? inv.getStockMinimo()
                    : (m.getStockMinimo() != null ? m.getStockMinimo() : 0);
            if (disp <= 0) {
                sinStock++;
                alertas.add(Map.of(
                        "medicamentoId", m.getId(),
                        "nombre", m.getNombreComercial(),
                        "disponible", disp,
                        "stockMinimo", min,
                        "nivel", "AGOTADO"
                ));
            } else if (min > 0 && disp <= min) {
                stockBajo++;
                alertas.add(Map.of(
                        "medicamentoId", m.getId(),
                        "nombre", m.getNombreComercial(),
                        "disponible", disp,
                        "stockMinimo", min,
                        "nivel", "BAJO"
                ));
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("proveedorId", id);
        out.put("productosActivos", productos);
        out.put("conStockBajo", stockBajo);
        out.put("agotados", sinStock);
        out.put("alertas", alertas);
        return ResponseEntity.ok(out);
    }
}
