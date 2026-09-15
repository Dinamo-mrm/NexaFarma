package com.nexafarma.service;

import com.nexafarma.entity.Empleado;
import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Lote;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.entity.MovimientoInventario;
import com.nexafarma.entity.TipoMovimientoInventario;
import com.nexafarma.repository.MedicamentoRepository;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.MovimientoInventarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final LoteService loteService;
    private final InventarioService inventarioService;
    private final MedicamentoRepository medicamentoRepository;
    private final JdbcTemplate jdbcTemplate;

    public MovimientoInventarioServiceImpl(MovimientoInventarioRepository movimientoRepository,
                                           EmpleadoRepository empleadoRepository,
                                           LoteService loteService,
                                           InventarioService inventarioService,
                                           MedicamentoRepository medicamentoRepository,
                                           JdbcTemplate jdbcTemplate) {
        this.movimientoRepository = movimientoRepository;
        this.empleadoRepository = empleadoRepository;
        this.loteService = loteService;
        this.inventarioService = inventarioService;
        this.medicamentoRepository = medicamentoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MovimientoInventario registrarEntrada(Long loteId, Integer cantidad, Long empleadoResponsableId, String motivo) {
        validarCantidad(cantidad);
        Lote lote = loteService.obtenerPorId(loteId);
        Empleado empleado = obtenerEmpleado(empleadoResponsableId);

        Long medicamentoId = lote.getMedicamento().getId();
        Inventario inventarioAntes = obtenerOInicializarInventario(medicamentoId);
        int existenciaAnterior = inventarioAntes.getCantidadDisponible();

        loteService.actualizarCantidadDisponible(loteId, lote.getCantidadDisponible() + cantidad);
        Inventario inventarioActualizado = inventarioService.ajustarCantidad(medicamentoId, cantidad);

        return guardarMovimiento(lote, TipoMovimientoInventario.ENTRADA, cantidad, empleado, motivo,
                existenciaAnterior, inventarioActualizado.getCantidadDisponible());
    }

    @Override
    public MovimientoInventario registrarSalida(Long loteId, Integer cantidad, Long empleadoResponsableId, String motivo) {
        validarCantidad(cantidad);
        // Reutiliza la validación de vigencia ya existente en LoteService (Punto 6/7): no duplica lógica.
        loteService.validarLoteVigente(loteId);

        Lote lote = loteService.obtenerPorId(loteId);
        Empleado empleado = obtenerEmpleado(empleadoResponsableId);

        if (lote.getCantidadDisponible() < cantidad) {
            throw new ReglaNegocioException(
                    "Cantidad insuficiente en el lote " + lote.getNumeroLote()
                            + " (disponible: " + lote.getCantidadDisponible() + ")");
        }

        Long medicamentoId = lote.getMedicamento().getId();
        if (!inventarioService.tieneStockSuficiente(medicamentoId, cantidad)) {
            throw new ReglaNegocioException("Stock insuficiente en inventario para el medicamento id " + medicamentoId);
        }

        Inventario inventarioAntes = inventarioService.obtenerPorMedicamento(medicamentoId);
        int existenciaAnterior = inventarioAntes.getCantidadDisponible();

        loteService.actualizarCantidadDisponible(loteId, lote.getCantidadDisponible() - cantidad);
        Inventario inventarioActualizado = inventarioService.ajustarCantidad(medicamentoId, -cantidad);

        return guardarMovimiento(lote, TipoMovimientoInventario.SALIDA, cantidad, empleado, motivo,
                existenciaAnterior, inventarioActualizado.getCantidadDisponible());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovimientoInventario> listarPorMedicamento(Long medicamentoId, Pageable pageable) {
        return movimientoRepository.findByMedicamentoIdOrderByFechaDesc(medicamentoId, pageable);
    }

    private MovimientoInventario guardarMovimiento(Lote lote, TipoMovimientoInventario tipo, int cantidad,
                                                   Empleado empleado, String motivo,
                                                   int existenciaAnterior, int nuevaExistencia) {
        MovimientoInventario movimiento = MovimientoInventario.builder()
                .medicamento(lote.getMedicamento())
                .tipoMovimiento(tipo)
                .cantidad(cantidad)
                .usuarioResponsable(empleado)
                .motivo(motivo)
                .existenciaAnterior(existenciaAnterior)
                .nuevaExistencia(nuevaExistencia)
                .build();
        return persistir(movimiento);
    }

    private Inventario obtenerOInicializarInventario(Long medicamentoId) {
        try {
            return inventarioService.obtenerPorMedicamento(medicamentoId);
        } catch (ResourceNotFoundException ex) {
            return inventarioService.ajustarCantidad(medicamentoId, 0);
        }
    }

    private Empleado obtenerEmpleado(Long empleadoId) {
        return empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id " + empleadoId));
    }


    @Override
    public MovimientoInventario registrarAjusteSinLote(Long medicamentoId, Integer cantidad,
                                                        TipoMovimientoInventario tipo,
                                                        Long empleadoResponsableId, String motivo) {
        validarCantidad(cantidad);
        if (tipo == null) {
            throw new ReglaNegocioException("Debe indicar el tipo de movimiento");
        }
        Empleado empleado = obtenerEmpleado(empleadoResponsableId);
        Medicamento medicamento = medicamentoRepository.findById(medicamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento no encontrado con id " + medicamentoId));

        int existenciaAnterior = 0;
        int nuevaExistencia = 0;
        try {
            Inventario inv = inventarioService.obtenerPorMedicamento(medicamentoId);
            existenciaAnterior = inv.getCantidadDisponible();
            nuevaExistencia = existenciaAnterior;
        } catch (ResourceNotFoundException ignored) {
            // sin inventario aún
        }

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .medicamento(medicamento)
                .tipoMovimiento(tipo)
                .cantidad(cantidad)
                .usuarioResponsable(empleado)
                .motivo(motivo)
                .existenciaAnterior(existenciaAnterior)
                .nuevaExistencia(nuevaExistencia)
                .build();
        return persistir(movimiento);
    }

    private void validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad del movimiento debe ser mayor a cero");
        }
    }

    private MovimientoInventario persistir(MovimientoInventario movimiento) {
        movimiento.setId(null);
        resyncSecuencia();
        return movimientoRepository.save(movimiento);
    }

    private void resyncSecuencia() {
        try {
            Long maxId = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(id), 0) FROM movimientos_inventario", Long.class);
            if (maxId == null) {
                maxId = 0L;
            }
            String seq = null;
            try {
                seq = jdbcTemplate.queryForObject(
                        "SELECT pg_get_serial_sequence('movimientos_inventario', 'id')", String.class);
            } catch (Exception ignored) {
                // ignore
            }
            if (seq == null || seq.isBlank()) {
                seq = "public.movimientos_inventario_id_seq";
            }
            if (maxId > 0) {
                jdbcTemplate.queryForObject("SELECT setval(?::regclass, ?, true)", Long.class, seq, maxId);
            } else {
                jdbcTemplate.queryForObject("SELECT setval(?::regclass, 1, false)", Long.class, seq);
            }
        } catch (Exception e) {
            // último recurso: setval por nombre fijo
            try {
                Long maxId = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(MAX(id), 0) FROM movimientos_inventario", Long.class);
                jdbcTemplate.execute(
                        "SELECT setval('movimientos_inventario_id_seq', "
                        + (maxId == null || maxId < 1 ? "1, false" : (maxId + ", true")) + ")");
            } catch (Exception ignored) {
                // el INSERT fallará con mensaje claro si sigue mal
            }
        }
    }
}
