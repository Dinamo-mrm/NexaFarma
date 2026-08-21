package com.nexafarma.service;

import com.nexafarma.dto.CompraItemRequest;
import com.nexafarma.dto.CompraRequest;
import com.nexafarma.dto.CompraResponse;
import com.nexafarma.dto.LoteRecibidoRequest;
import com.nexafarma.dto.RecibirCompraRequest;
import com.nexafarma.entity.Compra;
import com.nexafarma.entity.DetalleCompra;
import com.nexafarma.entity.Empleado;
import com.nexafarma.entity.EstadoCompra;
import com.nexafarma.entity.EstadoLote;
import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Lote;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.entity.MovimientoInventario;
import com.nexafarma.entity.Proveedor;
import com.nexafarma.entity.TipoMovimientoInventario;
import com.nexafarma.exception.EstadoInvalidoException;
import com.nexafarma.exception.RecursoNoEncontradoException;
import com.nexafarma.repository.CompraRepository;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.InventarioRepository;
import com.nexafarma.repository.LoteRepository;
import com.nexafarma.repository.MedicamentoRepository;
import com.nexafarma.repository.MovimientoInventarioRepository;
import com.nexafarma.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Módulo de Compras a proveedores. Responsabilidad del Desarrollador 3
 * (Transacciones, Frontend y Fórmulas).
 *
 * <p>El flujo tiene dos pasos: {@link #registrarCompra} crea la orden en
 * estado PENDIENTE (sin tocar inventario), y {@link #recibirCompra} registra
 * la llegada física de la mercancía -creando/actualizando Lotes y el
 * Inventario agregado, tal como lo hace VentaService en sentido inverso.</p>
 */
@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpleadoRepository empleadoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final LoteRepository loteRepository;
    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final GeneradorNumeroDocumento generadorNumeroDocumento;

    @Transactional
    public CompraResponse registrarCompra(CompraRequest request) {
        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Proveedor", request.proveedorId()));
        Empleado empleado = empleadoRepository.findById(request.empleadoResponsableId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Empleado", request.empleadoResponsableId()));

        Compra compra = Compra.builder()
                .numeroCompra(generadorNumeroDocumento.generar("CMP"))
                .proveedor(proveedor)
                .empleadoResponsable(empleado)
                .fechaCompra(LocalDateTime.now())
                .formaPago(request.formaPago())
                .estado(EstadoCompra.PENDIENTE)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CompraItemRequest item : request.items()) {
            Medicamento medicamento = medicamentoRepository.findById(item.medicamentoId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Medicamento", item.medicamentoId()));

            BigDecimal precioUnitario = item.precioUnitario() != null
                    ? item.precioUnitario()
                    : medicamento.getPrecioCompra();
            BigDecimal subtotalItem = precioUnitario.multiply(BigDecimal.valueOf(item.cantidad()));

            DetalleCompra detalle = DetalleCompra.builder()
                    .compra(compra)
                    .medicamento(medicamento)
                    .cantidad(item.cantidad())
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotalItem)
                    .build();
            compra.getDetalles().add(detalle);
            subtotal = subtotal.add(subtotalItem);
        }

        // Nota: los medicamentos no llevan IVA discriminado en este MVP; el
        // cálculo de impuestos puede ajustarse aquí según reglas fiscales.
        compra.setSubtotal(subtotal);
        compra.setImpuestos(BigDecimal.ZERO);
        compra.setTotal(subtotal);

        return CompraResponse.desde(compraRepository.save(compra));
    }

    /**
     * Registra la recepción física de una compra PENDIENTE: crea un Lote por
     * cada entrada informada, incrementa el Inventario agregado del
     * medicamento y deja rastro en MovimientoInventario (ENTRADA).
     */
    @Transactional
    public CompraResponse recibirCompra(Long compraId, RecibirCompraRequest request) {
        Compra compra = obtenerEntidad(compraId);
        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new EstadoInvalidoException(
                    "Solo se puede recibir una compra en estado PENDIENTE (estado actual: " + compra.getEstado() + ")");
        }

        Map<Long, DetalleCompra> detallesPorId = new HashMap<>();
        for (DetalleCompra detalle : compra.getDetalles()) {
            detallesPorId.put(detalle.getId(), detalle);
        }

        for (LoteRecibidoRequest loteReq : request.lotesRecibidos()) {
            DetalleCompra detalle = detallesPorId.get(loteReq.detalleCompraId());
            if (detalle == null) {
                throw new RecursoNoEncontradoException(
                        "El detalle de compra " + loteReq.detalleCompraId() + " no pertenece a la compra " + compraId);
            }
            registrarEntradaDeLote(detalle.getMedicamento(), loteReq, compra);
        }

        compra.setEstado(EstadoCompra.RECIBIDA);
        return CompraResponse.desde(compraRepository.save(compra));
    }

    private void registrarEntradaDeLote(Medicamento medicamento, LoteRecibidoRequest loteReq, Compra compra) {
        Lote lote = Lote.builder()
                .medicamento(medicamento)
                .numeroLote(loteReq.numeroLote())
                .fechaFabricacion(loteReq.fechaFabricacion())
                .fechaVencimiento(loteReq.fechaVencimiento())
                .cantidadDisponible(loteReq.cantidad())
                .estado(EstadoLote.ACTIVO)
                .build();
        loteRepository.save(lote);

        Inventario inventario = inventarioRepository.findByMedicamentoId(medicamento.getId())
                .orElseGet(() -> Inventario.builder()
                        .medicamento(medicamento)
                        .cantidadDisponible(0)
                        .stockMinimo(medicamento.getStockMinimo())
                        .build());

        int existenciaAnterior = inventario.getCantidadDisponible();
        inventario.setCantidadDisponible(existenciaAnterior + loteReq.cantidad());
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .medicamento(medicamento)
                .tipoMovimiento(TipoMovimientoInventario.ENTRADA)
                .cantidad(loteReq.cantidad())
                .usuarioResponsable(compra.getEmpleadoResponsable())
                .motivo("Recepción compra " + compra.getNumeroCompra() + " / lote " + loteReq.numeroLote())
                .existenciaAnterior(existenciaAnterior)
                .nuevaExistencia(inventario.getCantidadDisponible())
                .build();
        movimientoInventarioRepository.save(movimiento);
    }

    @Transactional
    public CompraResponse cancelarCompra(Long compraId) {
        Compra compra = obtenerEntidad(compraId);
        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new EstadoInvalidoException(
                    "Solo se puede cancelar una compra en estado PENDIENTE (estado actual: " + compra.getEstado() + ")");
        }
        compra.setEstado(EstadoCompra.CANCELADA);
        return CompraResponse.desde(compraRepository.save(compra));
    }

    @Transactional(readOnly = true)
    public CompraResponse obtenerPorId(Long id) {
        return CompraResponse.desde(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public Page<CompraResponse> listarPorEstado(EstadoCompra estado, Pageable pageable) {
        return compraRepository.findByEstado(estado, pageable).map(CompraResponse::desde);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listarPorProveedor(Long proveedorId) {
        return compraRepository.findByProveedorIdOrderByFechaCompraDesc(proveedorId)
                .stream().map(CompraResponse::desde).toList();
    }

    Compra obtenerEntidad(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Compra", id));
    }
}
