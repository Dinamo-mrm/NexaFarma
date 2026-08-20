package com.nexafarma.service;

import com.nexafarma.entity.Medicamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MedicamentoService {

    Medicamento crear(Medicamento medicamento);

    Medicamento obtenerPorId(Long id);

    Page<Medicamento> listarActivos(Pageable pageable);

    Page<Medicamento> buscarPorNombre(String nombre, Pageable pageable);

    Medicamento actualizar(Long id, Medicamento datosActualizados);

    void eliminar(Long id);
}