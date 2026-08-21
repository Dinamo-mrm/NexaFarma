package com.nexafarma.service;

import com.nexafarma.dto.DetalleFormulaRequest;
import com.nexafarma.dto.FormulaMedicaRequest;
import com.nexafarma.dto.FormulaMedicaResponse;
import com.nexafarma.entity.Cliente;
import com.nexafarma.entity.DetalleFormula;
import com.nexafarma.entity.FormulaMedica;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.exception.RecursoNoEncontradoException;
import com.nexafarma.repository.ClienteRepository;
import com.nexafarma.repository.FormulaMedicaRepository;
import com.nexafarma.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Módulo de Fórmulas Médicas. Responsabilidad del Desarrollador 3
 * (Transacciones, Frontend y Fórmulas). Ampara la venta de medicamentos
 * controlados/de prescripción; la vigencia se valida en VentaService al
 * momento de facturar.
 */
@Service
@RequiredArgsConstructor
public class FormulaMedicaService {

    private final FormulaMedicaRepository formulaMedicaRepository;
    private final ClienteRepository clienteRepository;
    private final MedicamentoRepository medicamentoRepository;

    @Transactional
    public FormulaMedicaResponse registrar(FormulaMedicaRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", request.clienteId()));

        FormulaMedica formula = FormulaMedica.builder()
                .cliente(cliente)
                .nombreMedico(request.nombreMedico())
                .numeroTarjetaProfesional(request.numeroTarjetaProfesional())
                .entidadSalud(request.entidadSalud())
                .fechaExpedicion(request.fechaExpedicion())
                .duracionTratamientoDias(request.duracionTratamientoDias())
                .archivoAdjuntoUrl(request.archivoAdjuntoUrl())
                .build();

        for (DetalleFormulaRequest detalleReq : request.detalles()) {
            Medicamento medicamento = medicamentoRepository.findById(detalleReq.medicamentoId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Medicamento", detalleReq.medicamentoId()));
            DetalleFormula detalle = DetalleFormula.builder()
                    .formulaMedica(formula)
                    .medicamento(medicamento)
                    .dosis(detalleReq.dosis())
                    .frecuencia(detalleReq.frecuencia())
                    .duracionTratamiento(detalleReq.duracionTratamiento())
                    .build();
            formula.getDetalles().add(detalle);
        }

        return FormulaMedicaResponse.desde(formulaMedicaRepository.save(formula));
    }

    @Transactional(readOnly = true)
    public FormulaMedicaResponse obtenerPorId(Long id) {
        return FormulaMedicaResponse.desde(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public List<FormulaMedicaResponse> listarPorCliente(Long clienteId) {
        return formulaMedicaRepository.findByClienteIdOrderByFechaExpedicionDesc(clienteId)
                .stream().map(FormulaMedicaResponse::desde).toList();
    }

    /** Consulta puntual de vigencia, útil para el frontend antes de armar la venta. */
    @Transactional(readOnly = true)
    public boolean esVigente(Long id) {
        return obtenerEntidad(id).esVigente();
    }

    FormulaMedica obtenerEntidad(Long id) {
        return formulaMedicaRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("FormulaMedica", id));
    }
}
