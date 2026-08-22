package com.nexafarma.service;

import com.nexafarma.entity.Empleado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmpleadoService {

    Empleado crear(Empleado empleado);

    Empleado obtenerPorId(Long id);

    Page<Empleado> listar(Pageable pageable);

    Page<Empleado> buscarPorNombre(String nombre, Pageable pageable);

    Empleado actualizar(Long id, Empleado datosActualizados);

    void eliminar(Long id);
}
