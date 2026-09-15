package com.nexafarma.service;

import com.nexafarma.entity.Categoria;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.entity.Proveedor;
import com.nexafarma.exception.DuplicateResourceException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.repository.CategoriaRepository;
import com.nexafarma.entity.PrecioHistorial;
import com.nexafarma.repository.MedicamentoRepository;
import com.nexafarma.repository.PrecioHistorialRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import com.nexafarma.repository.ProveedorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MedicamentoServiceImpl implements MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;
    private final PrecioHistorialRepository precioHistorialRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;

    public MedicamentoServiceImpl(MedicamentoRepository medicamentoRepository,
                                  CategoriaRepository categoriaRepository,
                                  ProveedorRepository proveedorRepository, PrecioHistorialRepository precioHistorialRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.precioHistorialRepository = precioHistorialRepository;
    }

    @Override
    public Medicamento crear(Medicamento medicamento) {
        validarDuplicados(medicamento.getCodigoInterno(), medicamento.getCodigoBarras(), null);
        Categoria categoria = resolverCategoria(medicamento);
        Proveedor proveedor = resolverProveedor(medicamento);
        validarReglasDePrecio(medicamento);

        medicamento.setId(null);
        medicamento.setCategoria(categoria);
        medicamento.setProveedor(proveedor);
        medicamento.setActivo(true);

        return medicamentoRepository.save(medicamento);
    }

    @Override
    @Transactional(readOnly = true)
    public Medicamento obtenerPorId(Long id) {
        return medicamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento no encontrado con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Medicamento> listarActivos(Pageable pageable) {
        return medicamentoRepository.listarActivosConProveedor(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Medicamento> buscarPorNombre(String nombre, Pageable pageable) {
        return medicamentoRepository.findByNombreComercialContainingIgnoreCaseAndActivoTrue(nombre, pageable);
    }

    @Override
    public Medicamento actualizar(Long id, Medicamento datosActualizados) {
        Medicamento existente = obtenerPorId(id);

        validarDuplicados(datosActualizados.getCodigoInterno(), datosActualizados.getCodigoBarras(), id);
        Categoria categoria = resolverCategoria(datosActualizados);
        Proveedor proveedor = resolverProveedor(datosActualizados);
        validarReglasDePrecio(datosActualizados);

        existente.setCodigoInterno(datosActualizados.getCodigoInterno());
        existente.setCodigoBarras(datosActualizados.getCodigoBarras());
        existente.setNombreComercial(datosActualizados.getNombreComercial());
        existente.setDescripcion(datosActualizados.getDescripcion());
        existente.setPresentacion(datosActualizados.getPresentacion());
        existente.setCategoria(categoria);
        existente.setProveedor(proveedor);
        existente.setPrecioCompra(datosActualizados.getPrecioCompra());
        BigDecimal precioAnterior = existente.getPrecioVenta();
        existente.setPrecioVenta(datosActualizados.getPrecioVenta());
        if (datosActualizados.getPrecioMaximoRegulado() != null
                || (existente.getPrecioMaximoRegulado() != null)) {
            existente.setPrecioMaximoRegulado(datosActualizados.getPrecioMaximoRegulado());
        }
        if (datosActualizados.getFactorBlister() != null) {
            existente.setFactorBlister(datosActualizados.getFactorBlister());
        }
        // Auditoría de cambio de precio
        if (precioAnterior != null && datosActualizados.getPrecioVenta() != null
                && precioAnterior.compareTo(datosActualizados.getPrecioVenta()) != 0) {
            String usuario = "sistema";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                usuario = auth.getName();
            }
            precioHistorialRepository.save(PrecioHistorial.builder()
                    .productoId(existente.getId())
                    .precioAnterior(precioAnterior)
                    .precioNuevo(datosActualizados.getPrecioVenta())
                    .precioMaximoRegulado(existente.getPrecioMaximoRegulado())
                    .usuario(usuario)
                    .build());
        }
        existente.setStockMinimo(datosActualizados.getStockMinimo());
        existente.setUbicacion(datosActualizados.getUbicacion());

        existente.setNombreGenerico(datosActualizados.getNombreGenerico());
        existente.setConcentracion(datosActualizados.getConcentracion());
        existente.setLaboratorioFabricante(datosActualizados.getLaboratorioFabricante());
        existente.setRegistroInvima(datosActualizados.getRegistroInvima());
        existente.setVentaLibre(datosActualizados.isVentaLibre());
        existente.setRequiereFormula(datosActualizados.isRequiereFormula());
        existente.setUsoControlado(datosActualizados.isUsoControlado());
        existente.setRequiereRefrigeracion(datosActualizados.isRequiereRefrigeracion());
        existente.setTieneRestricciones(datosActualizados.isTieneRestricciones());
        existente.setAptoFraccionamiento(datosActualizados.isAptoFraccionamiento());
        if (datosActualizados.getFactorConversion() != null) {
            existente.setFactorConversion(datosActualizados.getFactorConversion());
        }
        if (datosActualizados.getUnidadMinima() != null) {
            existente.setUnidadMinima(datosActualizados.getUnidadMinima());
        }
        if (datosActualizados.getPuntosPorUnidad() != null) {
            existente.setPuntosPorUnidad(datosActualizados.getPuntosPorUnidad());
        }

        return medicamentoRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        Medicamento existente = obtenerPorId(id);
        // Soft delete, mismo patrón usado en Proveedor/Cliente/Empleado.
        // El bloqueo de ventas de vencidos y las alertas se implementan en puntos posteriores.
        existente.setActivo(false);
        medicamentoRepository.save(existente);
    }

    // --- Validaciones básicas ---

    private void validarDuplicados(String codigoInterno, String codigoBarras, Long idActual) {
        medicamentoRepository.findByCodigoInterno(codigoInterno).ifPresent(m -> {
            if (idActual == null || !m.getId().equals(idActual)) {
                throw new DuplicateResourceException(
                        "Ya existe un medicamento con el código interno " + codigoInterno);
            }
        });
        medicamentoRepository.findByCodigoBarras(codigoBarras).ifPresent(m -> {
            if (idActual == null || !m.getId().equals(idActual)) {
                throw new DuplicateResourceException(
                        "Ya existe un medicamento con el código de barras " + codigoBarras);
            }
        });
    }

    private Categoria resolverCategoria(Medicamento medicamento) {
        if (medicamento.getCategoria() == null || medicamento.getCategoria().getId() == null) {
            throw new ResourceNotFoundException("Debe indicar una categoría válida para el medicamento");
        }
        return categoriaRepository.findById(medicamento.getCategoria().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría no encontrada con id " + medicamento.getCategoria().getId()));
    }

    private Proveedor resolverProveedor(Medicamento medicamento) {
        if (medicamento.getProveedor() == null || medicamento.getProveedor().getId() == null) {
            throw new ResourceNotFoundException("Debe indicar un proveedor válido para el medicamento");
        }
        return proveedorRepository.findById(medicamento.getProveedor().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proveedor no encontrado con id " + medicamento.getProveedor().getId()));
    }

    /** Regla básica de negocio: el precio de venta no puede ser menor o igual al de compra. */
    private void validarReglasDePrecio(Medicamento m) {
        if (m.getPrecioVenta() != null && m.getPrecioCompra() != null
                && m.getPrecioVenta().compareTo(m.getPrecioCompra()) < 0) {
            throw new ReglaNegocioException("El precio de venta no puede ser menor al precio de compra");
        }
        if (m.getPrecioMaximoRegulado() != null && m.getPrecioVenta() != null
                && m.getPrecioVenta().compareTo(m.getPrecioMaximoRegulado()) > 0) {
            throw new ReglaNegocioException(
                    "El precio de venta ($" + m.getPrecioVenta()
                    + ") supera el precio máximo regulado CNPMDM ($" + m.getPrecioMaximoRegulado() + ")");
        }
        if (m.getFactorBlister() != null && m.getFactorBlister() < 1) {
            throw new ReglaNegocioException("El factor blíster debe ser >= 1");
        }
        if (m.getFactorConversion() != null && m.getFactorBlister() != null
                && m.getFactorBlister() > 1 && m.getFactorConversion() > 1
                && m.getFactorConversion() % m.getFactorBlister() != 0) {
            throw new ReglaNegocioException(
                    "El factor de caja debe ser múltiplo del factor blíster (ej. caja 30, blíster 10)");
        }
    }
}