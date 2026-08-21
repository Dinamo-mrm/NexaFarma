package com.nexafarma.service;

import com.nexafarma.dto.VentaItemRequest;
import com.nexafarma.dto.VentaRequest;
import com.nexafarma.dto.VentaResponse;
import com.nexafarma.entity.Cliente;
import com.nexafarma.entity.DetalleFormula;
import com.nexafarma.entity.DetalleVenta;
import com.nexafarma.entity.Empleado;
import com.nexafarma.entity.EstadoVenta;
import com.nexafarma.entity.FormulaMedica;
import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Lote;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.entity.MovimientoInventario;
import com.nexafarma.entity.TipoMovimientoInventario;
import com.nexafarma.entity.Venta;
import com.nexafarma.entity.EstadoLote;
import com.nexafarma.exception.EstadoInvalidoException;
import com.nexafarma.exception.FormulaMedicaInvalidaException;
import com.nexafarma.exception.RecursoNoEncontradoException;
import com.nexafarma.exception.StockInsuficienteException;
import com.nexafarma.repository.ClienteRepository;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.FormulaMedicaRepository;
import com.nexafarma.repository.InventarioRepository;
import com.nexafarma.repository.LoteRepository;
import com.nexafarma.repository.MedicamentoRepository;
import com.nexafarma.repository.MovimientoInventarioRepository;
import com.nexafarma.repository.VentaRepository;
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
 * Módulo de Ventas (punto de venta). Responsabilidad del Desarrollador 3
 * (Transacciones, Frontend y Fórmulas). Es el módulo central del sistema:
 * aplica FEFO (First Expired, First Out) para descontar stock, bloquea
 * lotes vencidos y exige fórmula médica vigente para medicamentos
 * controlados/de prescripción.
 */
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final LoteRepository loteRepository;
    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final FormulaMedicaRepository formulaMedicaRepository;
    private final GeneradorNumeroDocumento generadorNumeroDocumento;

    @Transactional
    public VentaResponse crearVenta(VentaRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", request.clienteId()));
        Empleado empleado = empleadoRepository.findById(request.empleadoId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Empleado", request.empleadoId()));

        FormulaMedica formulaMedica = null;
        if (request.formulaMedicaId() != null) {
            formulaMedica = formulaMedicaRepository.findById(request.formulaMedicaId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("FormulaMedica", request.formulaMedicaId()));
        }

        Venta venta = Venta.builder()
                .numeroVenta(generadorNumeroDocumento.generar("VTA"))
                .cliente(cliente)
                .empleado(empleado)
                .fecha(LocalDateTime.now())
                .metodoPago(request.metodoPago())
                .descuento(request.descuento() != null ? request.descuento() : BigDecimal.ZERO)
                .estado(EstadoVenta.PENDIENTE)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (VentaItemRequest item : request.items()) {
            Medicamento medicamento = medicamentoRepository.findById(item.medicamentoId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Medicamento", item.medicamentoId()));

            validarFormulaSiRequerida(medicamento, formulaMedica);
            subtotal = subtotal.add(
                    descontarStockFefoYCrearDetalles(venta, medicamento, item.cantidad()));
        }

        BigDecimal descuento = venta.getDescuento();
        // Nota: los medicamentos no llevan IVA discriminado en este MVP; el
        // cálculo de impuestos puede ajustarse aquí según reglas fiscales.
        BigDecimal impuestos = BigDecimal.ZERO;
        venta.setSubtotal(subtotal);
        venta.setImpuestos(impuestos);
        venta.setTotal(subtotal.subtract(descuento).add(impuestos));
        venta.setEstado(EstadoVenta.PAGADA);

        return VentaResponse.desde(ventaRepository.save(venta));
    }

    private void validarFormulaSiRequerida(Medicamento medicamento, FormulaMedica formulaMedica) {
        if (!medicamento.isRequiereFormula()) {
            return;
        }
        if (formulaMedica == null) {
            throw new FormulaMedicaInvalidaException(
                    "El medicamento " + medicamento.getNombreComercial()
                            + " requiere fórmula médica y no se informó ninguna en la venta");
        }
        if (!formulaMedica.esVigente()) {
            throw new FormulaMedicaInvalidaException(
                    "La fórmula médica " + formulaMedica.getId() + " no está vigente");
        }
        boolean amparado = formulaMedica.getDetalles().stream()
                .map(DetalleFormula::getMedicamento)
                .anyMatch(m -> m.getId().equals(medicamento.getId()));
        if (!amparado) {
            throw new FormulaMedicaInvalidaException(
                    "La fórmula médica " + formulaMedica.getId()
                            + " no ampara el medicamento " + medicamento.getNombreComercial());
        }
    }

    /**
     * Estrategia FEFO: consume primero los lotes activos con vencimiento más
     * próximo. Puede generar varios DetalleVenta para un mismo medicamento
     * si la cantidad solicitada abarca más de un lote (trazabilidad exacta).
     * Bloquea automáticamente cualquier lote vencido.
     */
    private BigDecimal descontarStockFefoYCrearDetalles(Venta venta, Medicamento medicamento, int cantidadSolicitada) {
        List<Lote> lotesDisponibles = loteRepository
                .findByMedicamentoIdAndEstadoOrderByFechaVencimientoAsc(medicamento.getId(), EstadoLote.ACTIVO);

        int pendiente = cantidadSolicitada;
        BigDecimal subtotalMedicamento = BigDecimal.ZERO;
        BigDecimal precioUnitario = medicamento.getPrecioVenta();

        for (Lote lote : lotesDisponibles) {
            if (pendiente <= 0) {
                break;
            }
            // Bloqueo preventivo: nunca se vende un lote vencido, incluso si
            // su estado en BD aún no fue actualizado por el job de Dev2.
            if (lote.estaVencido() || lote.getCantidadDisponible() <= 0) {
                continue;
            }

            int cantidadTomada = Math.min(pendiente, lote.getCantidadDisponible());
            lote.setCantidadDisponible(lote.getCantidadDisponible() - cantidadTomada);
            loteRepository.save(lote);

            BigDecimal subtotalPortion = precioUnitario.multiply(BigDecimal.valueOf(cantidadTomada));
            DetalleVenta detalle = DetalleVenta.builder()
                    .venta(venta)
                    .medicamento(medicamento)
                    .lote(lote)
                    .cantidad(cantidadTomada)
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotalPortion)
                    .build();
            venta.getDetalles().add(detalle);

            subtotalMedicamento = subtotalMedicamento.add(subtotalPortion);
            pendiente -= cantidadTomada;
        }

        if (pendiente > 0) {
            throw new StockInsuficienteException(
                    "Stock insuficiente para " + medicamento.getNombreComercial()
                            + ": faltan " + pendiente + " unidades (lotes vencidos no se contabilizan)");
        }

        actualizarInventarioYMovimiento(medicamento, cantidadSolicitada, venta, TipoMovimientoInventario.SALIDA);
        return subtotalMedicamento;
    }

    private void actualizarInventarioYMovimiento(Medicamento medicamento, int cantidad, Venta venta,
                                                  TipoMovimientoInventario tipo) {
        Inventario inventario = inventarioRepository.findByMedicamentoId(medicamento.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe registro de Inventario para el medicamento " + medicamento.getId()));

        int existenciaAnterior = inventario.getCantidadDisponible();
        int nuevaExistencia = tipo == TipoMovimientoInventario.SALIDA
                ? existenciaAnterior - cantidad
                : existenciaAnterior + cantidad;
        inventario.setCantidadDisponible(nuevaExistencia);
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .medicamento(medicamento)
                .tipoMovimiento(tipo)
                .cantidad(cantidad)
                .usuarioResponsable(venta.getEmpleado())
                .motivo((tipo == TipoMovimientoInventario.SALIDA ? "Venta " : "Anulación venta ")
                        + venta.getNumeroVenta())
                .existenciaAnterior(existenciaAnterior)
                .nuevaExistencia(nuevaExistencia)
                .build();
        movimientoInventarioRepository.save(movimiento);
    }

    /** Anula una venta PAGADA: revierte cantidades a los lotes originales y al inventario agregado. */
    @Transactional
    public VentaResponse anularVenta(Long ventaId) {
        Venta venta = obtenerEntidad(ventaId);
        if (venta.getEstado() != EstadoVenta.PAGADA) {
            throw new EstadoInvalidoException(
                    "Solo se puede anular una venta en estado PAGADA (estado actual: " + venta.getEstado() + ")");
        }

        Map<Long, Integer> cantidadPorMedicamento = new HashMap<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            if (detalle.getLote() != null) {
                Lote lote = detalle.getLote();
                lote.setCantidadDisponible(lote.getCantidadDisponible() + detalle.getCantidad());
                loteRepository.save(lote);
            }
            cantidadPorMedicamento.merge(detalle.getMedicamento().getId(), detalle.getCantidad(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : cantidadPorMedicamento.entrySet()) {
            Medicamento medicamento = medicamentoRepository.findById(entry.getKey())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Medicamento", entry.getKey()));
            actualizarInventarioYMovimiento(medicamento, entry.getValue(), venta, TipoMovimientoInventario.ENTRADA);
        }

        venta.setEstado(EstadoVenta.ANULADA);
        return VentaResponse.desde(ventaRepository.save(venta));
    }

    @Transactional(readOnly = true)
    public VentaResponse obtenerPorId(Long id) {
        return VentaResponse.desde(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public Page<VentaResponse> listarPorEstado(EstadoVenta estado, Pageable pageable) {
        return ventaRepository.findByEstado(estado, pageable).map(VentaResponse::desde);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarPorCliente(Long clienteId) {
        return ventaRepository.findByClienteIdOrderByFechaDesc(clienteId)
                .stream().map(VentaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarPorEmpleadoYRango(Long empleadoId, LocalDateTime desde, LocalDateTime hasta) {
        return ventaRepository.findByEmpleadoIdAndFechaBetween(empleadoId, desde, hasta)
                .stream().map(VentaResponse::desde).toList();
    }

    Venta obtenerEntidad(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Venta", id));
    }
}
