package com.nexafarma.service;

import com.nexafarma.entity.Cliente;
import com.nexafarma.entity.DetalleFormula;
import com.nexafarma.entity.FormulaMedica;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.ClienteRepository;
import com.nexafarma.repository.FormulaMedicaRepository;
import com.nexafarma.repository.MedicamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FormulaMedicaServiceImpl implements FormulaMedicaService {

    private final FormulaMedicaRepository formulaMedicaRepository;
    private final ClienteRepository clienteRepository;
    private final MedicamentoRepository medicamentoRepository;

    public FormulaMedicaServiceImpl(FormulaMedicaRepository formulaMedicaRepository,
                                     ClienteRepository clienteRepository,
                                     MedicamentoRepository medicamentoRepository) {
        this.formulaMedicaRepository = formulaMedicaRepository;
        this.clienteRepository = clienteRepository;
        this.medicamentoRepository = medicamentoRepository;
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
            if (detalle.getMedicamento() == null || detalle.getMedicamento().getId() == null) {
                throw new ReglaNegocioException("Cada medicamento prescrito debe indicar su id");
            }
            // El JSON deserializado solo trae el id; se reemplaza por la
            // entidad completa para que la respuesta muestre el nombre real.
            Medicamento medicamento = medicamentoRepository.findById(detalle.getMedicamento().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Medicamento no encontrado con id " + detalle.getMedicamento().getId()));
            detalle.setMedicamento(medicamento);
            detalle.setFormulaMedica(formula);
        }

        formula.setId(null);
        return formulaMedicaRepository.save(formula);
    }

    @Override
    @Transactional(readOnly = true)
    public FormulaMedica obtenerPorId(Long id) {
        return formulaMedicaRepository.findDetalladaById(id)
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
