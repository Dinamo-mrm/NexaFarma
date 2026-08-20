package com.nexafarma.service;

import com.nexafarma.entity.Proveedor;
import com.nexafarma.exception.DuplicateResourceException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.ProveedorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    public Proveedor crear(Proveedor proveedor) {
        if (proveedorRepository.existsByNit(proveedor.getNit())) {
            throw new DuplicateResourceException("Ya existe un proveedor con el NIT " + proveedor.getNit());
        }
        proveedor.setId(null);
        proveedor.setActivo(true);
        return proveedorRepository.save(proveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public Proveedor obtenerPorId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Proveedor> listar(Pageable pageable) {
        return proveedorRepository.findByActivoTrue(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Proveedor> buscarPorRazonSocial(String razonSocial, Pageable pageable) {
        return proveedorRepository.findByRazonSocialContainingIgnoreCaseAndActivoTrue(razonSocial, pageable);
    }

    @Override
    public Proveedor actualizar(Long id, Proveedor datosActualizados) {
        Proveedor existente = obtenerPorId(id);

        if (!existente.getNit().equals(datosActualizados.getNit())
                && proveedorRepository.existsByNit(datosActualizados.getNit())) {
            throw new DuplicateResourceException("Ya existe un proveedor con el NIT " + datosActualizados.getNit());
        }

        existente.setNit(datosActualizados.getNit());
        existente.setRazonSocial(datosActualizados.getRazonSocial());
        existente.setNombreContacto(datosActualizados.getNombreContacto());
        existente.setTelefono(datosActualizados.getTelefono());
        existente.setCorreo(datosActualizados.getCorreo());
        existente.setDireccion(datosActualizados.getDireccion());

        return proveedorRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        Proveedor existente = obtenerPorId(id);
        // Soft delete, igual que Cliente/Empleado/Producto en este proyecto.
        existente.setActivo(false);
        proveedorRepository.save(existente);
    }
}