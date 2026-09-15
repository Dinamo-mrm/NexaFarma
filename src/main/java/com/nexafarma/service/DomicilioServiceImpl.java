package com.nexafarma.service;

import com.nexafarma.entity.*;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.ClienteRepository;
import com.nexafarma.repository.DomicilioRepository;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        // Cliente opcional: se puede despachar a consumidor final solo con dirección/teléfono.
        if (input.getDireccionEntrega() == null || input.getDireccionEntrega().isBlank()) {
            throw new ReglaNegocioException("Debe indicar la direccion de entrega");
        }

        if (input.getTelefonoContacto() == null || input.getTelefonoContacto().isBlank()) {
            throw new ReglaNegocioException("Debe indicar el telefono de contacto para el domicilio");
        }        if (domicilioRepository.findByVentaId(input.getVenta().getId()).isPresent()) {
            throw new ReglaNegocioException("Esta venta ya tiene un domicilio asociado");
        }

        Venta venta = ventaRepository.findDetalladaById(input.getVenta().getId())
                .orElseGet(() -> ventaRepository.findById(input.getVenta().getId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Venta no encontrada con id " + input.getVenta().getId())));
        Cliente cliente = null;
        if (input.getCliente() != null && input.getCliente().getId() != null) {
            cliente = clienteRepository.findById(input.getCliente().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cliente no encontrado con id " + input.getCliente().getId()));
        }

        boolean requiereTermico = detectarCadenaFrio(venta);

        Domicilio domicilio = Domicilio.builder()
                .venta(venta)
                .cliente(cliente)
                .direccionEntrega(input.getDireccionEntrega())
                .telefonoContacto(input.getTelefonoContacto())
                .valorDomicilio(input.getValorDomicilio())
                .estado(EstadoDomicilio.PENDIENTE)
                .requiereTransporteTermico(requiereTermico)
                .neveraPortatilConfirmada(false)
                .montoPagaCliente(input.getMontoPagaCliente())
                .build();

        // Si ya viene monto y la venta es efectivo, calcular cambio
        recalcularCambio(domicilio, venta);

        return domicilioRepository.save(domicilio);
    }

    private boolean detectarCadenaFrio(Venta venta) {
        if (venta.getDetalles() == null) {
            return false;
        }
        return venta.getDetalles().stream()
                .map(DetalleVenta::getMedicamento)
                .filter(m -> m != null)
                .anyMatch(Medicamento::isRequiereRefrigeracion);
    }

    private void recalcularCambio(Domicilio domicilio, Venta venta) {
        if (domicilio.getMontoPagaCliente() == null || venta == null || venta.getTotal() == null) {
            domicilio.setCambioEnRuta(null);
            return;
        }
        BigDecimal totalCobrar = venta.getTotal();
        if (domicilio.getValorDomicilio() != null) {
            totalCobrar = totalCobrar.add(domicilio.getValorDomicilio());
        }
        if (domicilio.getMontoPagaCliente().compareTo(totalCobrar) < 0) {
            throw new ReglaNegocioException(
                    "El monto con que paga el cliente es menor al total a cobrar (" + totalCobrar + ")");
        }
        domicilio.setCambioEnRuta(domicilio.getMontoPagaCliente().subtract(totalCobrar));
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
    public Domicilio asignarDomiciliario(Long domicilioId, Long domiciliarioId, boolean confirmarNeveraPortatil) {
        Domicilio domicilio = obtenerPorId(domicilioId);
        Empleado domiciliario = empleadoRepository.findById(domiciliarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empleado no encontrado con id " + domiciliarioId));

        if (domicilio.isRequiereTransporteTermico() && !confirmarNeveraPortatil) {
            throw new ReglaNegocioException(
                    "Este pedido REQUIERE TRANSPORTE TÉRMICO. Confirme el uso de nevera portátil antes de asignar domiciliario.");
        }
        if (confirmarNeveraPortatil) {
            domicilio.setNeveraPortatilConfirmada(true);
        }

        domicilio.setDomiciliario(domiciliario);
        domicilio.setEstado(EstadoDomicilio.EN_PREPARACION);
        return domicilioRepository.save(domicilio);
    }

    @Override
    public Domicilio registrarPagoEfectivo(Long domicilioId, BigDecimal montoPagaCliente) {
        Domicilio domicilio = obtenerPorId(domicilioId);
        if (domicilio.getEstado() != EstadoDomicilio.PENDIENTE
                && domicilio.getEstado() != EstadoDomicilio.EN_PREPARACION) {
            throw new ReglaNegocioException("Solo se registra pago en efectivo en Pendiente o En preparación");
        }
        Venta venta = domicilio.getVenta();
        if (venta != null && venta.getId() != null) {
            venta = ventaRepository.findById(venta.getId()).orElse(venta);
        }
        domicilio.setMontoPagaCliente(montoPagaCliente);
        recalcularCambio(domicilio, venta);
        return domicilioRepository.save(domicilio);
    }

    @Override
    public Domicilio marcarEnCamino(Long domicilioId) {
        Domicilio domicilio = obtenerPorId(domicilioId);
        if (domicilio.getDomiciliario() == null) {
            throw new ReglaNegocioException("El domicilio no tiene domiciliario asignado");
        }
        if (domicilio.isRequiereTransporteTermico() && !domicilio.isNeveraPortatilConfirmada()) {
            throw new ReglaNegocioException(
                    "No se puede despachar: falta confirmar nevera portátil (cadena de frío)");
        }
        if (domicilio.getEstado() != EstadoDomicilio.EN_PREPARACION) {
            throw new ReglaNegocioException("Solo un domicilio EN_PREPARACION puede pasar a EN_CAMINO");
        }
        domicilio.setEstado(EstadoDomicilio.EN_CAMINO);
        domicilio.setHoraSalida(LocalDateTime.now());
        return domicilioRepository.save(domicilio);
    }

    @Override
    public Domicilio marcarEntregado(Long domicilioId, String evidenciaEntregaUrl, String firmaDigitalUrl) {
        Domicilio domicilio = obtenerPorId(domicilioId);
        if (domicilio.getEstado() != EstadoDomicilio.EN_CAMINO) {
            throw new ReglaNegocioException("Solo un domicilio EN_CAMINO puede marcarse ENTREGADO");
        }
        boolean tieneEvidencia = (evidenciaEntregaUrl != null && !evidenciaEntregaUrl.isBlank())
                || (firmaDigitalUrl != null && !firmaDigitalUrl.isBlank());
        if (!tieneEvidencia) {
            throw new ReglaNegocioException(
                    "Proof of Delivery obligatorio: adjunte foto de la guía firmada o firma digital");
        }
        domicilio.setEvidenciaEntregaUrl(evidenciaEntregaUrl);
        domicilio.setFirmaDigitalUrl(firmaDigitalUrl);
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
