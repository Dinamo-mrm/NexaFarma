package com.nexafarma.service;

import com.nexafarma.entity.*;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.CompraRepository;
import com.nexafarma.repository.DevolucionRepository;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.MedicamentoRepository;
import com.nexafarma.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Nota de diseno: la entidad Devolucion referencia el Medicamento pero no un
 * Lote especifico (una devolucion de cliente puede no traer informacion de
 * lote). Por eso el reingreso/descuento de stock se hace directamente sobre
 * Inventario (InventarioService.ajustarCantidad) en vez de pasar por
 * MovimientoInventarioService, que exige un loteId. Si el equipo necesita
 * trazabilidad por lote en devoluciones, se puede agregar un campo
 * `lote` opcional a la entidad Devolucion mas adelante.
 */
@Service
@Transactional
public class DevolucionServiceImpl implements DevolucionService {

    private final DevolucionRepository devolucionRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final EmpleadoRepository empleadoRepository;
    private final InventarioService inventarioService;

    public DevolucionServiceImpl(DevolucionRepository devolucionRepository,
                                  MedicamentoRepository medicamentoRepository,
                                  VentaRepository ventaRepository,
                                  CompraRepository compraRepository,
                                  EmpleadoRepository empleadoRepository,
                                  InventarioService inventarioService) {
        this.devolucionRepository = devolucionRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.ventaRepository = ventaRepository;
        this.compraRepository = compraRepository;
        this.empleadoRepository = empleadoRepository;
        this.inventarioService = inventarioService;
    }

    @Override
    public Devolucion crear(Devolucion input) {
        if (input.getTipo() == null) {
            throw new ReglaNegocioException("Debe indicar el tipo de devolucion (CLIENTE o PROVEEDOR)");
        }
        if (input.getMedicamento() == null || input.getMedicamento().getId() == null) {
            throw new ReglaNegocioException("Debe indicar el medicamento devuelto");
        }
        if (input.getCantidad() == null || input.getCantidad() <= 0) {
            throw new ReglaNegocioException("La cantidad devuelta debe ser mayor a cero");
        }
        if (input.getMotivo() == null) {
            throw new ReglaNegocioException("Debe indicar el motivo de la devolucion");
        }

        Medicamento medicamento = medicamentoRepository.findById(input.getMedicamento().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medicamento no encontrado con id " + input.getMedicamento().getId()));

        Venta ventaOrigen = null;
        if (input.getVentaOrigen() != null && input.getVentaOrigen().getId() != null) {
            ventaOrigen = ventaRepository.findById(input.getVentaOrigen().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venta origen no encontrada"));
        }
        Compra compraOrigen = null;
        if (input.getCompraOrigen() != null && input.getCompraOrigen().getId() != null) {
            compraOrigen = compraRepository.findById(input.getCompraOrigen().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Compra origen no encontrada"));
        }

        Devolucion devolucion = Devolucion.builder()
                .tipo(input.getTipo())
                .ventaOrigen(ventaOrigen)
                .compraOrigen(compraOrigen)
                .medicamento(medicamento)
                .cantidad(input.getCantidad())
                .motivo(input.getMotivo())
                .estadoProducto(input.getEstadoProducto())
                .validadoPorFarmaceutico(false)
                .build();

        return devolucionRepository.save(devolucion);
    }

    @Override
    @Transactional(readOnly = true)
    public Devolucion obtenerPorId(Long id) {
        return devolucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolucion no encontrada con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Devolucion> listarPendientesDeValidacion() {
        return devolucionRepository.findByValidadoPorFarmaceuticoFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Devolucion> listarPorTipo(TipoDevolucion tipo) {
        return devolucionRepository.findByTipo(tipo);
    }

    @Override
    public Devolucion validar(Long devolucionId, Long farmaceuticoId) {
        Devolucion devolucion = obtenerPorId(devolucionId);
        if (devolucion.isValidadoPorFarmaceutico()) {
            throw new ReglaNegocioException("La devolucion " + devolucionId + " ya fue validada");
        }

        Empleado farmaceutico = empleadoRepository.findById(farmaceuticoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id " + farmaceuticoId));

        // Regla de negocio: solo al validar se impacta el inventario, nunca antes.
        if (devolucion.getTipo() == TipoDevolucion.CLIENTE) {
            inventarioService.ajustarCantidad(devolucion.getMedicamento().getId(), devolucion.getCantidad());
        } else {
            inventarioService.ajustarCantidad(devolucion.getMedicamento().getId(), -devolucion.getCantidad());
        }

        devolucion.setValidadoPorFarmaceutico(true);
        devolucion.setValidadoPor(farmaceutico);
        return devolucionRepository.save(devolucion);
    }
}
