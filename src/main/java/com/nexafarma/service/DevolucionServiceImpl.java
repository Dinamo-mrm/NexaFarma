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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class DevolucionServiceImpl implements DevolucionService {

    private static final Set<MotivoDevolucion> MOTIVOS_REINGRESO = EnumSet.of(
            MotivoDevolucion.ERROR_DIGITACION,
            MotivoDevolucion.ERROR_EN_LA_ENTREGA
    );

    private static final Set<MotivoDevolucion> MOTIVOS_BAJA = EnumSet.of(
            MotivoDevolucion.PRODUCTO_DEFECTUOSO,
            MotivoDevolucion.PRODUCTO_VENCIDO,
            MotivoDevolucion.PRODUCTO_PROXIMO_A_VENCER,
            MotivoDevolucion.DANO_EN_EL_EMPAQUE,
            MotivoDevolucion.RETIRO_DEL_MERCADO
    );

    private final DevolucionRepository devolucionRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final EmpleadoRepository empleadoRepository;
    private final InventarioService inventarioService;
    private final MovimientoInventarioService movimientoInventarioService;

    public DevolucionServiceImpl(DevolucionRepository devolucionRepository,
                                  MedicamentoRepository medicamentoRepository,
                                  VentaRepository ventaRepository,
                                  CompraRepository compraRepository,
                                  EmpleadoRepository empleadoRepository,
                                  InventarioService inventarioService,
                                  MovimientoInventarioService movimientoInventarioService) {
        this.devolucionRepository = devolucionRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.ventaRepository = ventaRepository;
        this.compraRepository = compraRepository;
        this.empleadoRepository = empleadoRepository;
        this.inventarioService = inventarioService;
        this.movimientoInventarioService = movimientoInventarioService;
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empleado no encontrado con id " + farmaceuticoId));

        Long medId = devolucion.getMedicamento().getId();
        int cantidad = devolucion.getCantidad();
        MotivoDevolucion motivo = devolucion.getMotivo();
        String motivoTxt = "Devolucion #" + devolucionId + " / " + motivo;

        if (devolucion.getTipo() == TipoDevolucion.CLIENTE) {
            if (MOTIVOS_REINGRESO.contains(motivo)) {
                inventarioService.ajustarCantidad(medId, cantidad);
                movimientoInventarioService.registrarAjusteSinLote(
                        medId, cantidad, TipoMovimientoInventario.ENTRADA,
                        farmaceuticoId, motivoTxt + " — reingreso a stock");
            } else {
                // Averiado / caducado / daño: no reingresa a inventario vendible
                movimientoInventarioService.registrarAjusteSinLote(
                        medId, cantidad, TipoMovimientoInventario.SALIDA_BAJA,
                        farmaceuticoId, motivoTxt + " — no reingresa a venta");
            }
        } else {
            inventarioService.ajustarCantidad(medId, -cantidad);
            movimientoInventarioService.registrarAjusteSinLote(
                    medId, cantidad, TipoMovimientoInventario.SALIDA,
                    farmaceuticoId, motivoTxt + " — devolucion a proveedor");
        }

        devolucion.setValidadoPorFarmaceutico(true);
        devolucion.setValidadoPor(farmaceutico);
        return devolucionRepository.save(devolucion);
    }
}
