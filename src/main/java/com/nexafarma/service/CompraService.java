package com.nexafarma.service;

import com.nexafarma.entity.Compra;
import com.nexafarma.entity.EstadoCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompraService {

    /** Registra la orden de compra en estado PENDIENTE (aun no impacta inventario). */
    Compra crear(Compra compra);

    Compra obtenerPorId(Long id);

    Page<Compra> listar(Pageable pageable);

    Page<Compra> listarPorEstado(EstadoCompra estado, Pageable pageable);

    /**
     * Marca la compra como RECIBIDA y, por cada detalle, crea el Lote
     * correspondiente y registra la entrada en inventario
     * (via MovimientoInventarioService.registrarEntrada).
     */
    Compra recibir(Long compraId, java.util.Map<Long, DatosLoteRecepcion> datosLotePorDetalle);

    void cancelar(Long compraId);

    record DatosLoteRecepcion(String numeroLote, java.time.LocalDate fechaFabricacion, java.time.LocalDate fechaVencimiento) {
    }
}
