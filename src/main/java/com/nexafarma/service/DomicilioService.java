package com.nexafarma.service;

import com.nexafarma.entity.Domicilio;
import com.nexafarma.entity.EstadoDomicilio;

import java.math.BigDecimal;
import java.util.List;

public interface DomicilioService {

    Domicilio crear(Domicilio domicilio);

    Domicilio obtenerPorId(Long id);

    List<Domicilio> listarPorEstado(EstadoDomicilio estado);

    Domicilio asignarDomiciliario(Long domicilioId, Long domiciliarioId, boolean confirmarNeveraPortatil);

    Domicilio registrarPagoEfectivo(Long domicilioId, BigDecimal montoPagaCliente);

    Domicilio marcarEnCamino(Long domicilioId);

    /** PoD obligatorio: evidencia (foto) o firma digital. */
    Domicilio marcarEntregado(Long domicilioId, String evidenciaEntregaUrl, String firmaDigitalUrl);

    Domicilio cancelar(Long domicilioId);
}
