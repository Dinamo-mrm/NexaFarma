package com.nexafarma.service;

import com.nexafarma.dto.ClienteRequest;
import com.nexafarma.dto.ClienteResponse;
import com.nexafarma.entity.Cliente;
import com.nexafarma.exception.RecursoDuplicadoException;
import com.nexafarma.exception.RecursoNoEncontradoException;
import com.nexafarma.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Módulo de Clientes. Responsabilidad del Desarrollador 3
 * (Transacciones, Frontend y Fórmulas).
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        if (clienteRepository.existsByDocumento(request.documento())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un cliente registrado con el documento " + request.documento());
        }
        Cliente cliente = Cliente.builder()
                .documento(request.documento())
                .nombreCompleto(request.nombreCompleto())
                .telefono(request.telefono())
                .correo(request.correo())
                .direccion(request.direccion())
                .fechaNacimiento(request.fechaNacimiento())
                .eps(request.eps())
                .alergias(request.alergias())
                .activo(true)
                .build();
        return ClienteResponse.desde(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = obtenerEntidad(id);

        if (!cliente.getDocumento().equals(request.documento())
                && clienteRepository.existsByDocumento(request.documento())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un cliente registrado con el documento " + request.documento());
        }

        cliente.setDocumento(request.documento());
        cliente.setNombreCompleto(request.nombreCompleto());
        cliente.setTelefono(request.telefono());
        cliente.setCorreo(request.correo());
        cliente.setDireccion(request.direccion());
        cliente.setFechaNacimiento(request.fechaNacimiento());
        cliente.setEps(request.eps());
        cliente.setAlergias(request.alergias());
        return ClienteResponse.desde(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorId(Long id) {
        return ClienteResponse.desde(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(String nombre, Pageable pageable) {
        String filtro = nombre == null ? "" : nombre;
        return clienteRepository
                .findByNombreCompletoContainingIgnoreCaseAndActivoTrue(filtro, pageable)
                .map(ClienteResponse::desde);
    }

    /** Baja lógica: se conserva el historial de ventas/fórmulas asociado al cliente. */
    @Transactional
    public void desactivar(Long id) {
        Cliente cliente = obtenerEntidad(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    Cliente obtenerEntidad(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", id));
    }
}
