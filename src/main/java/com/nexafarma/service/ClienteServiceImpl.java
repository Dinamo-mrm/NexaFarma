package com.nexafarma.service;

import com.nexafarma.entity.Cliente;
import com.nexafarma.exception.DuplicateResourceException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.ClienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Cliente crear(Cliente cliente) {
        if (clienteRepository.existsByDocumento(cliente.getDocumento())) {
            throw new DuplicateResourceException("Ya existe un cliente con el documento " + cliente.getDocumento());
        }
        cliente.setId(null);
        cliente.setActivo(true);
        // Habeas Data: se persiste el valor enviado (check obligatorio en UI)
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente obtenerPorDocumento(String documento) {
        return clienteRepository.findByDocumento(documento)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con documento " + documento));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Cliente> listar(Pageable pageable) {
        return clienteRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Cliente> buscarPorNombre(String nombre, Pageable pageable) {
        return clienteRepository.findByNombreCompletoContainingIgnoreCaseAndActivoTrue(nombre, pageable);
    }

    @Override
    public Cliente actualizar(Long id, Cliente datosActualizados) {
        Cliente existente = obtenerPorId(id);

        if (!existente.getDocumento().equals(datosActualizados.getDocumento())
                && clienteRepository.existsByDocumento(datosActualizados.getDocumento())) {
            throw new DuplicateResourceException(
                    "Ya existe un cliente con el documento " + datosActualizados.getDocumento());
        }

        if (datosActualizados.getTipoDocumento() != null) existente.setTipoDocumento(datosActualizados.getTipoDocumento());
        if (datosActualizados.getEmailFacturacion() != null) existente.setEmailFacturacion(datosActualizados.getEmailFacturacion());
        if (datosActualizados.getResponsabilidadTributaria() != null) existente.setResponsabilidadTributaria(datosActualizados.getResponsabilidadTributaria());
        existente.setDocumento(datosActualizados.getDocumento());
        existente.setNombreCompleto(datosActualizados.getNombreCompleto());
        existente.setTelefono(datosActualizados.getTelefono());
        existente.setCorreo(datosActualizados.getCorreo());
        existente.setDireccion(datosActualizados.getDireccion());
        existente.setFechaNacimiento(datosActualizados.getFechaNacimiento());
        existente.setEps(datosActualizados.getEps());
        existente.setAlergias(datosActualizados.getAlergias());
        existente.setAutorizaTratamientoDatos(datosActualizados.isAutorizaTratamientoDatos());

        return clienteRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        Cliente existente = obtenerPorId(id);
        existente.setActivo(false);
        clienteRepository.save(existente);
    }
}
