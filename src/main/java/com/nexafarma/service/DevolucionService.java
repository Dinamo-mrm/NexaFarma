package com.nexafarma.service;

import com.nexafarma.entity.Devolucion;
import com.nexafarma.entity.TipoDevolucion;

import java.util.List;

public interface DevolucionService {

    /** Registra la devolucion en estado pendiente de validacion (no toca inventario todavia). */
    Devolucion crear(Devolucion devolucion);

    Devolucion obtenerPorId(Long id);

    List<Devolucion> listarPendientesDeValidacion();

    List<Devolucion> listarPorTipo(TipoDevolucion tipo);

    /**
     * Requiere validacion del farmaceutico antes de reingresar (CLIENTE) o
     * descontar (PROVEEDOR) el producto del inventario. Lanza
     * DevolucionNoValidadaException-equivalente (ReglaNegocioException) si
     * ya estaba validada.
     */
    Devolucion validar(Long devolucionId, Long farmaceuticoId);
}
