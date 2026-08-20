package com.nexafarma.service;

import com.nexafarma.entity.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProveedorService {

    Proveedor crear(Proveedor proveedor);

    Proveedor obtenerPorId(Long id);

    Page<Proveedor> listar(Pageable pageable);

    Page<Proveedor> buscarPorRazonSocial(String razonSocial, Pageable pageable);

    Proveedor actualizar(Long id, Proveedor datosActualizados);

    void eliminar(Long id);
}