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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    private final CrmService crmService;

    public VentaServiceImpl(VentaRepository ventaRepository,
                             ClienteRepository clienteRepository,
                             EmpleadoRepository empleadoRepository,
                             MedicamentoRepository medicamentoRepository,
                             LoteService loteService,
                             MovimientoInventarioService movimientoInventarioService,
                             FormulaMedicaService formulaMedicaService,
                              @Lazy CrmService crmService) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.loteService = loteService;
        this.movimientoInventarioService = movimientoInventarioService;
        this.formulaMedicaService = formulaMedicaService;
        this.crmService = crmService;
    }

    @Override
    public Venta crear(Venta ventaInput) {
        // Cliente opcional: null = venta anónima / consumidor final en mostrador.
        // Obligatorio solo si hay medicamentos que requieren fórmula o controlados.
        if (ventaInput.getEmpleado() == null || ventaInput.getEmpleado().getId() == null) {
            throw new ReglaNegocioException("Debe indicar el empleado que atiende la venta");
        }
        if (ventaInput.getDetalles() == null || ventaInput.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("La venta debe tener al menos un medicamento");
        }

        Cliente cliente = null;
        if (ventaInput.getCliente() != null && ventaInput.getCliente().getId() != null) {
            cliente = clienteRepository.findById(ventaInput.getCliente().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cliente no encontrado con id " + ventaInput.getCliente().getId()));
        }
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
        boolean requiereCliente = false;
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
            if ((medicamento.isRequiereFormula() || medicamento.isUsoControlado()) && cliente == null) {
                throw new ReglaNegocioException(
                        "Debe registrar o seleccionar un cliente para vender medicamentos con fórmula o de control especial");
            }

            // Fraccionamiento INVIMA: si no es apto, la cantidad debe ser múltiplo del factor de conversión (caja completa).
            int factor = medicamento.getFactorConversion() != null && medicamento.getFactorConversion() > 0
                    ? medicamento.getFactorConversion() : 1;
            if (!medicamento.isAptoFraccionamiento() && solicitud.getCantidad() % factor != 0) {
                throw new ReglaNegocioException(
                        "El medicamento " + medicamento.getNombreComercial()
                        + " no admite fraccionamiento (INVIMA). Venda en múltiplos de "
                        + factor + " " + (medicamento.getUnidadMinima() != null ? medicamento.getUnidadMinima() : "unidades"));
            }

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
        BigDecimal total = subtotal.subtract(venta.getDescuento()).add(impuestos);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        venta.setTotal(total);

        // Pagos mixtos: si vienen varios pagos, validar cobertura del total
        if (ventaInput.getPagos() != null && !ventaInput.getPagos().isEmpty()) {
            BigDecimal sumaPagos = BigDecimal.ZERO;
            for (PagoVenta p : ventaInput.getPagos()) {
                if (p.getMetodoPago() == null || p.getMonto() == null
                        || p.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ReglaNegocioException("Cada pago debe tener método y monto mayor a 0");
                }
                sumaPagos = sumaPagos.add(p.getMonto());
            }
            if (sumaPagos.compareTo(total) < 0) {
                throw new ReglaNegocioException(
                        "La suma de pagos (" + sumaPagos + ") es menor al total de la venta (" + total + ")");
            }
            if (ventaInput.getPagos().size() > 1) {
                venta.setMetodoPago(MetodoPago.PAGO_MIXTO);
            }
            for (PagoVenta p : ventaInput.getPagos()) {
                p.setId(null);
                p.setVenta(venta);
            }
            venta.setPagos(new ArrayList<>(ventaInput.getPagos()));
        }

        Venta guardada = ventaRepository.save(venta);
        try {
            if (crmService != null) {
                crmService.acumularPuntosPorVenta(guardada.getId());
            }
        } catch (Exception ignored) {
            // No bloquear la venta por fallos de CRM
        }
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public Venta obtenerPorId(Long id) {
        return ventaRepository.findDetalladaById(id)
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
