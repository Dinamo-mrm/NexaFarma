package com.nexafarma.service;

import com.nexafarma.entity.*;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.CompraRepository;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.MedicamentoRepository;
import com.nexafarma.repository.ProveedorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@Transactional
public class CompraServiceImpl implements CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpleadoRepository empleadoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final LoteService loteService;
    private final MovimientoInventarioService movimientoInventarioService;
    private final InventarioService inventarioService;

    public CompraServiceImpl(CompraRepository compraRepository,
                              ProveedorRepository proveedorRepository,
                              EmpleadoRepository empleadoRepository,
                              MedicamentoRepository medicamentoRepository,
                              LoteService loteService,
                              MovimientoInventarioService movimientoInventarioService,
                              InventarioService inventarioService) {
        this.compraRepository = compraRepository;
        this.proveedorRepository = proveedorRepository;
        this.empleadoRepository = empleadoRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.loteService = loteService;
        this.movimientoInventarioService = movimientoInventarioService;
        this.inventarioService = inventarioService;
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
            if (detalle.getMedicamento() == null || detalle.getMedicamento().getId() == null) {
                throw new ReglaNegocioException("Cada item de la compra debe indicar el medicamento");
            }
            // El JSON deserializado solo trae el id del medicamento; se
            // reemplaza por la entidad completa para que la respuesta y el
            // Lote que se cree al recibir la compra tengan los datos reales.
            Medicamento medicamento = medicamentoRepository.findById(detalle.getMedicamento().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Medicamento no encontrado con id " + detalle.getMedicamento().getId()));
            detalle.setMedicamento(medicamento);

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
        return compraRepository.findDetalladaById(id)
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

            // Recepción técnica: el lote entra en CUARENTENA. No es vendible hasta
            // que el Regente libere el lote (liberarCuarentena). La cantidad se registra
            // en el lote; el inventario vendible solo suma lotes ACTIVO.
            Lote lote = loteService.crearEnCuarentena(Lote.builder()
                    .medicamento(detalle.getMedicamento())
                    .numeroLote(datos.numeroLote())
                    .fechaFabricacion(datos.fechaFabricacion())
                    .fechaVencimiento(datos.fechaVencimiento())
                    .cantidadDisponible(0)
                    .build());

            movimientoInventarioService.registrarEntrada(lote.getId(), detalle.getCantidad(),
                    compra.getEmpleadoResponsable().getId(),
                    "Recepcion compra " + compra.getNumeroCompra() + " (CUARENTENA)");

            actualizarCostoPromedioPonderado(
                    detalle.getMedicamento().getId(),
                    detalle.getCantidad(),
                    detalle.getPrecioUnitario());
        }

        compra.setEstado(EstadoCompra.RECIBIDA);
        return compraRepository.save(compra);
    }

    /**
     * Costo promedio ponderado: (stockPrevio * costoAnterior + qty * precioUnitario)
     * / (stockPrevio + qty). No reemplaza a ciegas el precio de compra anterior.
     */
    private void actualizarCostoPromedioPonderado(Long medicamentoId, int cantidadNueva, BigDecimal precioUnitario) {
        if (medicamentoId == null || cantidadNueva <= 0 || precioUnitario == null) {
            return;
        }
        Medicamento med = medicamentoRepository.findById(medicamentoId).orElse(null);
        if (med == null) {
            return;
        }
        BigDecimal costoAnterior = med.getPrecioCompra() != null ? med.getPrecioCompra() : precioUnitario;

        int stockDespues = 0;
        try {
            stockDespues = inventarioService.obtenerPorMedicamento(medicamentoId).getCantidadDisponible();
        } catch (ResourceNotFoundException ignored) {
            stockDespues = cantidadNueva;
        }
        int stockPrevio = Math.max(0, stockDespues - cantidadNueva);

        if (stockPrevio == 0) {
            med.setPrecioCompra(precioUnitario.setScale(2, RoundingMode.HALF_UP));
        } else {
            BigDecimal valorAnterior = costoAnterior.multiply(BigDecimal.valueOf(stockPrevio));
            BigDecimal valorNuevo = precioUnitario.multiply(BigDecimal.valueOf(cantidadNueva));
            BigDecimal cpp = valorAnterior.add(valorNuevo)
                    .divide(BigDecimal.valueOf(stockPrevio + cantidadNueva), 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            med.setPrecioCompra(cpp);
        }
        medicamentoRepository.save(med);
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
