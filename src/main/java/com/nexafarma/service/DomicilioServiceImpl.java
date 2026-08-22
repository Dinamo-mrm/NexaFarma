package com.nexafarma.service;

import com.nexafarma.entity.Cliente;
import com.nexafarma.entity.Domicilio;
import com.nexafarma.entity.Empleado;
import com.nexafarma.entity.EstadoDomicilio;
import com.nexafarma.entity.Venta;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.ClienteRepository;
import com.nexafarma.repository.DomicilioRepository;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DomicilioServiceImpl implements DomicilioService {

    private final DomicilioRepository domicilioRepository;
    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;

    public DomicilioServiceImpl(DomicilioRepository domicilioRepository,
                                 VentaRepository ventaRepository,
                                 ClienteRepository clienteRepository,
                                 EmpleadoRepository empleadoRepository) {
        this.domicilioRepository = domicilioRepository;
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    public Domicilio crear(Domicilio input) {
        if (input.getVenta() == null || input.getVenta().getId() == null) {
            throw new ReglaNegocioException("Debe indicar la venta asociada al domicilio");
        }
        if (input.getCliente() == null || input.getCliente().getId() == null) {
            throw new ReglaNegocioException("Debe indicar el cliente que recibe el domicilio");
        }
        if (input.getDireccionEntrega() == null || input.getDireccionEntrega().isBlank()) {
            throw new ReglaNegocioException("Debe indicar la direccion de entrega");
        }
        if (domicilioRepository.findByVentaId(input.getVenta().getId()).isPresent()) {
            throw new ReglaNegocioException("Esta venta ya tiene un domicilio asociado");
        }

        Venta venta = ventaRepository.findById(input.getVenta().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id " + input.getVenta().getId()));
        Cliente cliente = clienteRepository.findById(input.getCliente().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id " + input.getCliente().getId()));

        Domicilio domicilio = Domicilio.builder()
                .venta(venta)
                .cliente(cliente)
                .direccionEntrega(input.getDireccionEntrega())
                .telefonoContacto(input.getTelefonoContacto())
                .valorDomicilio(input.getValorDomicilio())
                .estado(EstadoDomicilio.PENDIENTE)
                .build();

        return domicilioRepository.save(domicilio);
    }

    @Override
    @Transactional(readOnly = true)
    public Domicilio obtenerPorId(Long id) {
        return domicilioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domicilio no encontrado con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Domicilio> listarPorEstado(EstadoDomicilio estado) {
        return domicilioRepository.findByEstado(estado);
    }

    @Override
    public Domicilio asignarDomiciliario(Long domicilioId, Long domiciliarioId) {
        Domicilio domicilio = obtenerPorId(domicilioId);
        Empleado domiciliario = empleadoRepository.findById(domiciliarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id " + domiciliarioId));

        domicilio.setDomiciliario(domiciliario);
        domicilio.setEstado(EstadoDomicilio.EN_PREPARACION);
        return domicilioRepository.save(domicilio);
    }

    @Override
    public Domicilio marcarEnCamino(Long domicilioId) {
        Domicilio domicilio = obtenerPorId(domicilioId);
        if (domicilio.getDomiciliario() == null) {
            throw new ReglaNegocioException("El domicilio no tiene domiciliario asignado");
        }
        if (domicilio.getEstado() != EstadoDomicilio.EN_PREPARACION) {
            throw new ReglaNegocioException("Solo un domicilio EN_PREPARACION puede pasar a EN_CAMINO");
        }
        domicilio.setEstado(EstadoDomicilio.EN_CAMINO);
        domicilio.setHoraSalida(LocalDateTime.now());
        return domicilioRepository.save(domicilio);
    }

    @Override
    public Domicilio marcarEntregado(Long domicilioId) {
        Domicilio domicilio = obtenerPorId(domicilioId);
        if (domicilio.getEstado() != EstadoDomicilio.EN_CAMINO) {
            throw new ReglaNegocioException("Solo un domicilio EN_CAMINO puede marcarse ENTREGADO");
        }
        domicilio.setEstado(EstadoDomicilio.ENTREGADO);
        domicilio.setHoraEntrega(LocalDateTime.now());
        return domicilioRepository.save(domicilio);
    }

    @Override
    public Domicilio cancelar(Long domicilioId) {
        Domicilio domicilio = obtenerPorId(domicilioId);
        if (domicilio.getEstado() == EstadoDomicilio.ENTREGADO) {
            throw new ReglaNegocioException("No se puede cancelar un domicilio ya ENTREGADO");
        }
        domicilio.setEstado(EstadoDomicilio.CANCELADO);
        return domicilioRepository.save(domicilio);
    }
}
