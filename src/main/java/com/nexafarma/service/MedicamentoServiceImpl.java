package com.nexafarma.service;

import com.nexafarma.entity.Categoria;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.entity.Proveedor;
import com.nexafarma.exception.DuplicateResourceException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.CategoriaRepository;
import com.nexafarma.repository.MedicamentoRepository;
import com.nexafarma.repository.ProveedorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MedicamentoServiceImpl implements MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;

    public MedicamentoServiceImpl(MedicamentoRepository medicamentoRepository,
                                  CategoriaRepository categoriaRepository,
                                  ProveedorRepository proveedorRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
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
        return medicamentoRepository.findByActivoTrue(pageable);
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
        existente.setPrecioVenta(datosActualizados.getPrecioVenta());
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
    private void validarReglasDePrecio(Medicamento medicamento) {
        if (medicamento.getPrecioVenta() != null && medicamento.getPrecioCompra() != null
                && medicamento.getPrecioVenta().compareTo(medicamento.getPrecioCompra()) <= 0) {
            throw new IllegalArgumentException(
                    "El precio de venta debe ser mayor al precio de compra");
        }
    }
}