package com.nexafarma.service;

import com.nexafarma.entity.*;
import com.nexafarma.exception.ReglaNegocioException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.AlertaRecompraRepository;
import com.nexafarma.repository.ClienteRepository;
import com.nexafarma.repository.VentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CrmServiceImpl implements CrmService {

    private static final Logger log = LoggerFactory.getLogger(CrmServiceImpl.class);

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final AlertaRecompraRepository alertaRecompraRepository;

    public CrmServiceImpl(VentaRepository ventaRepository,
                           ClienteRepository clienteRepository,
                           AlertaRecompraRepository alertaRecompraRepository) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.alertaRecompraRepository = alertaRecompraRepository;
    }

    @Override
    public void acumularPuntosPorVenta(Long ventaId) {
        Venta venta = ventaRepository.findDetalladaById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
        Cliente cliente = venta.getCliente();
        if (cliente == null) return;
        // Solo contactar / acumular si hay Habeas Data
        if (!cliente.isAutorizaTratamientoDatos()) {
            log.debug("Cliente {} sin autorización Habeas Data: no se acumulan puntos de marketing", cliente.getId());
            return;
        }
        int puntos = 0;
        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                Medicamento m = d.getMedicamento();
                if (m == null || m.isUsoControlado()) continue;
                int ptsUnit = m.getPuntosPorUnidad() != null ? m.getPuntosPorUnidad() : 0;
                puntos += ptsUnit * (d.getCantidad() != null ? d.getCantidad() : 0);
            }
        }
        if (puntos > 0) {
            cliente.setPuntosFidelizacion(cliente.getPuntosFidelizacion() + puntos);
            clienteRepository.save(cliente);
        }
    }

    @Override
    public Cliente redimirPuntos(Long clienteId, int puntos) {
        if (puntos <= 0) throw new ReglaNegocioException("Los puntos a redimir deben ser mayores a 0");
        Cliente c = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        if (c.getPuntosFidelizacion() < puntos) {
            throw new ReglaNegocioException("Saldo de puntos insuficiente");
        }
        c.setPuntosFidelizacion(c.getPuntosFidelizacion() - puntos);
        return clienteRepository.save(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaRecompra> listarRecomprasPendientes() {
        return alertaRecompraRepository.findByContactadoFalseOrderByFechaSugeridaAsc();
    }

    @Override
    public AlertaRecompra marcarContactado(Long alertaId) {
        AlertaRecompra a = alertaRecompraRepository.findById(alertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada"));
        a.setContactado(true);
        return alertaRecompraRepository.save(a);
    }

    /**
     * Diario 07:00 — para ventas de hace ~25 días con medicamentos de tratamiento,
     * crea alerta de recompra si el cliente autorizó datos.
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void jobRecompras() {
        int n = generarAlertasRecompra();
        log.info("Alertas de recompra generadas: {}", n);
    }

    @Override
    public int generarAlertasRecompra() {
        LocalDate objetivo = LocalDate.now();
        // Ventas de hace 25 días (tratamiento típico 30d → llamar día 25)
        var desde = objetivo.minusDays(25).atStartOfDay();
        var hasta = objetivo.minusDays(24).atStartOfDay();
        List<Venta> ventas = ventaRepository.findVentasPagadasEntre(desde, hasta);
        int creadas = 0;
        for (Venta v : ventas) {
            if (v.getCliente() == null || !v.getCliente().isAutorizaTratamientoDatos()) continue;
            Venta full = ventaRepository.findDetalladaById(v.getId()).orElse(v);
            if (full.getDetalles() == null) continue;
            for (DetalleVenta d : full.getDetalles()) {
                if (d.getMedicamento() == null) continue;
                AlertaRecompra alerta = AlertaRecompra.builder()
                        .cliente(full.getCliente())
                        .medicamento(d.getMedicamento())
                        .ventaOrigen(full)
                        .fechaSugerida(objetivo)
                        .contactado(false)
                        .build();
                alertaRecompraRepository.save(alerta);
                creadas++;
            }
        }
        return creadas;
    }
}
