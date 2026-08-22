package com.nexafarma.service;

import com.nexafarma.entity.*;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.ClienteRepository;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.MedicamentoRepository;
import com.nexafarma.repository.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final LoteService loteService;
    private final MovimientoInventarioService movimientoInventarioService;
    private final FormulaMedicaService formulaMedicaService;

    public VentaServiceImpl(VentaRepository ventaRepository,
                             ClienteRepository clienteRepository,
                             EmpleadoRepository empleadoRepository,
                             MedicamentoRepository medicamentoRepository,
                             LoteService loteService,
                             MovimientoInventarioService movimientoInventarioService,
                             FormulaMedicaService formulaMedicaService) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.loteService = loteService;
        this.movimientoInventarioService = movimientoInventarioService;
        this.formulaMedicaService = formulaMedicaService;
    }

    @Override
    public Venta crear(Venta ventaInput) {
        if (ventaInput.getCliente() == null || ventaInput.getCliente().getId() == null) {
            throw new ReglaNegocioException("Debe indicar el cliente de la venta");
        }
        if (ventaInput.getEmpleado() == null || ventaInput.getEmpleado().getId() == null) {
            throw new ReglaNegocioException("Debe indicar el empleado que atiende la venta");
        }
        if (ventaInput.getDetalles() == null || ventaInput.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("La venta debe tener al menos un medicamento");
        }

        Cliente cliente = clienteRepository.findById(ventaInput.getCliente().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con id " + ventaInput.getCliente().getId()));
        Empleado empleado = empleadoRepository.findById(ventaInput.getEmpleado().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empleado no encontrado con id " + ventaInput.getEmpleado().getId()));

        String numeroVenta = "V-" + String.format("%06d", ventaRepository.count() + 1);

        Venta venta = Venta.builder()
                .numeroVenta(numeroVenta)
                .cliente(cliente)
                .empleado(empleado)
                .metodoPago(ventaInput.getMetodoPago() != null ? ventaInput.getMetodoPago() : MetodoPago.EFECTIVO)
                .descuento(ventaInput.getDescuento() != null ? ventaInput.getDescuento() : BigDecimal.ZERO)
                .estado(EstadoVenta.PAGADA)
                .build();

        List<DetalleVenta> detallesFinales = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (DetalleVenta solicitud : ventaInput.getDetalles()) {
            if (solicitud.getMedicamento() == null || solicitud.getMedicamento().getId() == null) {
                throw new ReglaNegocioException("Cada item de la venta debe indicar el medicamento");
            }
            if (solicitud.getCantidad() == null || solicitud.getCantidad() <= 0) {
                throw new ReglaNegocioException("La cantidad de cada item debe ser mayor a cero");
            }

            Medicamento medicamento = medicamentoRepository.findById(solicitud.getMedicamento().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Medicamento no encontrado con id " + solicitud.getMedicamento().getId()));

            // Regla de negocio: medicamentos que requieren formula no se venden sin una vigente.
            if (medicamento.isRequiereFormula()) {
                formulaMedicaService.validarFormulaParaVenta(cliente.getId(), medicamento.getId());
            }

            BigDecimal precioUnitario = medicamento.getPrecioVenta();
            int restante = solicitud.getCantidad();

            // Descuento FEFO: primero los lotes mas proximos a vencer. Los lotes vencidos
            // ya no aparecen aqui porque LoteService.listarActivosFefo solo trae EstadoLote.ACTIVO,
            // y registrarSalida vuelve a validar vigencia antes de descontar (doble bloqueo).
            List<Lote> lotesFefo = loteService.listarActivosFefo(medicamento.getId());
            for (Lote lote : lotesFefo) {
                if (restante <= 0) break;
                int disponible = lote.getCantidadDisponible();
                if (disponible <= 0) continue;
                int tomar = Math.min(restante, disponible);

                movimientoInventarioService.registrarSalida(lote.getId(), tomar, empleado.getId(),
                        "Venta " + numeroVenta);

                BigDecimal subtotalDetalle = precioUnitario.multiply(BigDecimal.valueOf(tomar));
                detallesFinales.add(DetalleVenta.builder()
                        .venta(venta)
                        .medicamento(medicamento)
                        .lote(lote)
                        .cantidad(tomar)
                        .precioUnitario(precioUnitario)
                        .subtotal(subtotalDetalle)
                        .build());

                subtotal = subtotal.add(subtotalDetalle);
                restante -= tomar;
            }

            if (restante > 0) {
                throw new ReglaNegocioException(
                        "Stock insuficiente de " + medicamento.getNombreComercial()
                                + " (faltan " + restante + " unidades disponibles en lotes vigentes)");
            }
        }

        venta.setDetalles(detallesFinales);
        venta.setSubtotal(subtotal);
        BigDecimal impuestos = ventaInput.getImpuestos() != null ? ventaInput.getImpuestos() : BigDecimal.ZERO;
        venta.setImpuestos(impuestos);
        venta.setTotal(subtotal.subtract(venta.getDescuento()).add(impuestos));

        return ventaRepository.save(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public Venta obtenerPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Venta> listar(Pageable pageable) {
        return ventaRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Venta> listarPorEstado(EstadoVenta estado, Pageable pageable) {
        return ventaRepository.findByEstado(estado, pageable);
    }

    @Override
    public void anular(Long ventaId) {
        Venta venta = obtenerPorId(ventaId);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new ReglaNegocioException("La venta " + venta.getNumeroVenta() + " ya esta anulada");
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            if (detalle.getLote() != null) {
                movimientoInventarioService.registrarEntrada(detalle.getLote().getId(), detalle.getCantidad(),
                        venta.getEmpleado().getId(), "Anulacion venta " + venta.getNumeroVenta());
            }
        }

        venta.setEstado(EstadoVenta.ANULADA);
        ventaRepository.save(venta);
    }
}
