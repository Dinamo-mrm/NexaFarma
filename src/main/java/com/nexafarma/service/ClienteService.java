package com.nexafarma.service;

import com.nexafarma.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClienteService {

    Cliente crear(Cliente cliente);

    Cliente obtenerPorId(Long id);

    Cliente obtenerPorDocumento(String documento);

    Page<Cliente> listar(Pageable pageable);

    Page<Cliente> buscarPorNombre(String nombre, Pageable pageable);

    Cliente actualizar(Long id, Cliente datosActualizados);

    void eliminar(Long id);
}
