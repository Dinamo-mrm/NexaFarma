package com.nexafarma.service;

import com.nexafarma.entity.Empleado;
import com.nexafarma.exception.DuplicateResourceException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.EmpleadoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmpleadoServiceImpl implements EmpleadoService {

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoServiceImpl(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    public Empleado crear(Empleado empleado) {
        if (empleadoRepository.existsByDocumento(empleado.getDocumento())) {
            throw new DuplicateResourceException("Ya existe un empleado con el documento " + empleado.getDocumento());
        }
        empleado.setId(null);
        empleado.setActivo(true);
        return empleadoRepository.save(empleado);
    }

    @Override
    @Transactional(readOnly = true)
    public Empleado obtenerPorId(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Empleado> listar(Pageable pageable) {
        return empleadoRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Empleado> buscarPorNombre(String nombre, Pageable pageable) {
        return empleadoRepository.findByNombreCompletoContainingIgnoreCase(nombre, pageable);
    }

    @Override
    public Empleado actualizar(Long id, Empleado datosActualizados) {
        Empleado existente = obtenerPorId(id);

        if (!existente.getDocumento().equals(datosActualizados.getDocumento())
                && empleadoRepository.existsByDocumento(datosActualizados.getDocumento())) {
            throw new DuplicateResourceException(
                    "Ya existe un empleado con el documento " + datosActualizados.getDocumento());
        }

        existente.setDocumento(datosActualizados.getDocumento());
        existente.setNombreCompleto(datosActualizados.getNombreCompleto());
        existente.setCorreo(datosActualizados.getCorreo());
        existente.setTelefono(datosActualizados.getTelefono());
        existente.setDireccion(datosActualizados.getDireccion());
        existente.setCargo(datosActualizados.getCargo());
        existente.setSalario(datosActualizados.getSalario());
        existente.setFechaIngreso(datosActualizados.getFechaIngreso());

        return empleadoRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        Empleado existente = obtenerPorId(id);
        existente.setActivo(false);
        empleadoRepository.save(existente);
    }
}
