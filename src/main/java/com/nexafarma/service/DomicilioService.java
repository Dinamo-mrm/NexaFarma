package com.nexafarma.service;

import com.nexafarma.entity.Domicilio;
import com.nexafarma.entity.EstadoDomicilio;

import java.util.List;

public interface DomicilioService {

    Domicilio crear(Domicilio domicilio);

    Domicilio obtenerPorId(Long id);

    List<Domicilio> listarPorEstado(EstadoDomicilio estado);

    /** Asigna (o reasigna) un domiciliario y pasa el estado a EN_PREPARACION. */
    Domicilio asignarDomiciliario(Long domicilioId, Long domiciliarioId);

    Domicilio marcarEnCamino(Long domicilioId);

    Domicilio marcarEntregado(Long domicilioId);

    Domicilio cancelar(Long domicilioId);
}
