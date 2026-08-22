package com.nexafarma.service;

import com.nexafarma.entity.Cliente;
import com.nexafarma.entity.DetalleFormula;
import com.nexafarma.entity.FormulaMedica;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.ClienteRepository;
import com.nexafarma.repository.FormulaMedicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FormulaMedicaServiceImpl implements FormulaMedicaService {

    private final FormulaMedicaRepository formulaMedicaRepository;
    private final ClienteRepository clienteRepository;

    public FormulaMedicaServiceImpl(FormulaMedicaRepository formulaMedicaRepository,
                                     ClienteRepository clienteRepository) {
        this.formulaMedicaRepository = formulaMedicaRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public FormulaMedica crear(FormulaMedica formula) {
        if (formula.getCliente() == null || formula.getCliente().getId() == null) {
            throw new ReglaNegocioException("Debe indicar el cliente de la formula medica");
        }
        Cliente cliente = clienteRepository.findById(formula.getCliente().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con id " + formula.getCliente().getId()));
        formula.setCliente(cliente);

        if (formula.getDetalles() == null || formula.getDetalles().isEmpty()) {
            throw new ReglaNegocioException("La formula medica debe tener al menos un medicamento prescrito");
        }
        for (DetalleFormula detalle : formula.getDetalles()) {
            detalle.setFormulaMedica(formula);
        }

        formula.setId(null);
        return formulaMedicaRepository.save(formula);
    }

    @Override
    @Transactional(readOnly = true)
    public FormulaMedica obtenerPorId(Long id) {
        return formulaMedicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formula medica no encontrada con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormulaMedica> listarPorCliente(Long clienteId) {
        return formulaMedicaRepository.findByClienteIdOrderByFechaExpedicionDesc(clienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public FormulaMedica validarFormulaParaVenta(Long clienteId, Long medicamentoId) {
        List<FormulaMedica> formulas = formulaMedicaRepository.findByClienteIdOrderByFechaExpedicionDesc(clienteId);

        return formulas.stream()
                .filter(FormulaMedica::esVigente)
                .filter(f -> f.getDetalles().stream()
                        .anyMatch(d -> d.getMedicamento().getId().equals(medicamentoId)))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "El cliente no tiene una formula medica vigente que ampare este medicamento controlado"));
    }
}
