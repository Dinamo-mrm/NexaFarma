package com.nexafarma.service;

import com.nexafarma.entity.*;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.CompraRepository;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.ProveedorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Transactional
public class CompraServiceImpl implements CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpleadoRepository empleadoRepository;
    private final LoteService loteService;
    private final MovimientoInventarioService movimientoInventarioService;

    public CompraServiceImpl(CompraRepository compraRepository,
                              ProveedorRepository proveedorRepository,
                              EmpleadoRepository empleadoRepository,
                              LoteService loteService,
                              MovimientoInventarioService movimientoInventarioService) {
        this.compraRepository = compraRepository;
        this.proveedorRepository = proveedorRepository;
        this.empleadoRepository = empleadoRepository;
        this.loteService = loteService;
        this.movimientoInventarioService = movimientoInventarioService;
    }

    @Override
    public Compra crear(Compra compraInput) {
        if (compraInput.getProveedor() == null || compraInput.getProveedor().getId() == null) {
            throw new ReglaNegocioException("Debe indicar el proveedor de la compra");
        }
        if (compraInput.getEmpleadoResponsable() == null || compraInput.getEmpleadoResponsable().getId() == null) {
            throw new ReglaNegocioException("Debe indicar el empleado responsable de la compra");
        }
        if (compraInput.getDetalles() == null || compraInput.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("La compra debe tener al menos un medicamento");
        }

        Proveedor proveedor = proveedorRepository.findById(compraInput.getProveedor().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proveedor no encontrado con id " + compraInput.getProveedor().getId()));
        Empleado empleado = empleadoRepository.findById(compraInput.getEmpleadoResponsable().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empleado no encontrado con id " + compraInput.getEmpleadoResponsable().getId()));

        String numeroCompra = "C-" + String.format("%06d", compraRepository.count() + 1);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleCompra detalle : compraInput.getDetalles()) {
            if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                throw new ReglaNegocioException("La cantidad de cada item de la compra debe ser mayor a cero");
            }
            BigDecimal subtotalDetalle = detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()));
            detalle.setSubtotal(subtotalDetalle);
            subtotal = subtotal.add(subtotalDetalle);
        }

        Compra compra = Compra.builder()
                .numeroCompra(numeroCompra)
                .proveedor(proveedor)
                .empleadoResponsable(empleado)
                .formaPago(compraInput.getFormaPago() != null ? compraInput.getFormaPago() : MetodoPago.TRANSFERENCIA)
                .estado(EstadoCompra.PENDIENTE)
                .subtotal(subtotal)
                .build();

        BigDecimal impuestos = compraInput.getImpuestos() != null ? compraInput.getImpuestos() : BigDecimal.ZERO;
        compra.setImpuestos(impuestos);
        compra.setTotal(subtotal.add(impuestos));

        for (DetalleCompra detalle : compraInput.getDetalles()) {
            detalle.setCompra(compra);
        }
        compra.setDetalles(compraInput.getDetalles());

        return compraRepository.save(compra);
    }

    @Override
    @Transactional(readOnly = true)
    public Compra obtenerPorId(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Compra> listar(Pageable pageable) {
        return compraRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Compra> listarPorEstado(EstadoCompra estado, Pageable pageable) {
        return compraRepository.findByEstado(estado, pageable);
    }

    @Override
    public Compra recibir(Long compraId, Map<Long, DatosLoteRecepcion> datosLotePorDetalle) {
        Compra compra = obtenerPorId(compraId);
        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new ReglaNegocioException(
                    "Solo se puede recibir una compra en estado PENDIENTE (actual: " + compra.getEstado() + ")");
        }

        for (DetalleCompra detalle : compra.getDetalles()) {
            DatosLoteRecepcion datos = datosLotePorDetalle.get(detalle.getId());
            if (datos == null) {
                throw new ReglaNegocioException(
                        "Faltan los datos de lote (numero, fabricacion, vencimiento) para el detalle " + detalle.getId());
            }

            Lote lote = loteService.crear(Lote.builder()
                    .medicamento(detalle.getMedicamento())
                    .numeroLote(datos.numeroLote())
                    .fechaFabricacion(datos.fechaFabricacion())
                    .fechaVencimiento(datos.fechaVencimiento())
                    .cantidadDisponible(0)
                    .build());

            movimientoInventarioService.registrarEntrada(lote.getId(), detalle.getCantidad(),
                    compra.getEmpleadoResponsable().getId(), "Recepcion compra " + compra.getNumeroCompra());
        }

        compra.setEstado(EstadoCompra.RECIBIDA);
        return compraRepository.save(compra);
    }

    @Override
    public void cancelar(Long compraId) {
        Compra compra = obtenerPorId(compraId);
        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new ReglaNegocioException(
                    "Solo se puede cancelar una compra en estado PENDIENTE (actual: " + compra.getEstado() + ")");
        }
        compra.setEstado(EstadoCompra.CANCELADA);
        compraRepository.save(compra);
    }
}
