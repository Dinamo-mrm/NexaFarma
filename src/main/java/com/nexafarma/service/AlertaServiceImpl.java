package com.nexafarma.service;

import com.nexafarma.entity.EstadoLote;
import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Lote;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.repository.DetalleVentaRepository;
import com.nexafarma.repository.InventarioRepository;
import com.nexafarma.repository.LoteRepository;
import com.nexafarma.repository.MedicamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertaServiceImpl implements AlertaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaServiceImpl.class);

    private final InventarioService inventarioService;
    private final LoteService loteService;
    private final LoteRepository loteRepository;
    private final InventarioRepository inventarioRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    public AlertaServiceImpl(InventarioService inventarioService,
                              LoteService loteService,
                              LoteRepository loteRepository,
                              InventarioRepository inventarioRepository,
                              MedicamentoRepository medicamentoRepository,
                              DetalleVentaRepository detalleVentaRepository) {
        this.inventarioService = inventarioService;
        this.loteService = loteService;
        this.loteRepository = loteRepository;
        this.inventarioRepository = inventarioRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventario> obtenerAlertasStockBajo() {
        return inventarioService.listarStockBajo();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> obtenerAlertasProximosAVencer(Integer dias) {
        return loteService.listarProximosAVencer(dias);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lote> obtenerLotesVencidos() {
        return loteService.listarVencidos();
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void jobRevisionNocturna() {
        Map<String, Object> resumen = ejecutarRevisionNocturna();
        log.info("Revisión nocturna de lotes: {}", resumen);
    }

    @Scheduled(cron = "0 0 1 1 * *")
    @Transactional
    public void jobStockMinimoMensual() {
        int aplicados = aplicarSugerenciasStockMinimo();
        log.info("Stock mínimo dinámico aplicado a {} productos", aplicados);
    }

    @Override
    @Transactional
    public Map<String, Object> ejecutarRevisionNocturna() {
        LocalDate hoy = LocalDate.now();
        List<Lote> vencidosActivos = loteRepository.findVencidosNoActualizados();
        int marcados = 0;
        for (Lote lote : vencidosActivos) {
            if (lote.getEstado() == EstadoLote.ACTIVO || lote.getEstado() == EstadoLote.CUARENTENA) {
                lote.setEstado(EstadoLote.VENCIDO);
                loteRepository.save(lote);
                marcados++;
            }
        }

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("fecha", hoy.toString());
        resumen.put("lotesMarcadosVencidos", marcados);
        resumen.put("alertas30dias", loteRepository.findProximosAVencer(hoy.plusDays(30)).size());
        resumen.put("alertas60dias", loteRepository.findProximosAVencer(hoy.plusDays(60)).size());
        resumen.put("alertas90dias", loteRepository.findProximosAVencer(hoy.plusDays(90)).size());
        resumen.put("stockBajo", inventarioService.listarStockBajo().size());
        return resumen;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> sugerirStockMinimoDinamico() {
        LocalDateTime desde = LocalDateTime.now().minusDays(30);
        List<Object[]> filas = detalleVentaRepository.sumarCantidadesVendidasDesde(desde);
        List<Map<String, Object>> sugerencias = new ArrayList<>();

        for (Object[] fila : filas) {
            Long medicamentoId = ((Number) fila[0]).longValue();
            long unidadesVendidas = ((Number) fila[1]).longValue();
            int sugerido = (int) Math.ceil((unidadesVendidas / 30.0) * 7 * 1.2);
            if (sugerido < 5) {
                sugerido = 5;
            }

            var invOpt = inventarioRepository.findByMedicamentoId(medicamentoId);
            int actual = invOpt.map(Inventario::getStockMinimo).orElse(10);
            String nombre = invOpt.map(i -> i.getMedicamento() != null
                    ? i.getMedicamento().getNombreComercial() : null).orElse("#" + medicamentoId);
            if (sugerido == actual) {
                continue;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("medicamentoId", medicamentoId);
            row.put("nombre", nombre);
            row.put("stockMinimoActual", actual);
            row.put("stockMinimoSugerido", sugerido);
            row.put("unidadesVendidas30d", unidadesVendidas);
            sugerencias.add(row);
        }
        return sugerencias;
    }

    @Override
    @Transactional
    public int aplicarSugerenciasStockMinimo() {
        List<Map<String, Object>> sugerencias = sugerirStockMinimoDinamico();
        int aplicados = 0;
        for (Map<String, Object> s : sugerencias) {
            Long medId = ((Number) s.get("medicamentoId")).longValue();
            int sugerido = ((Number) s.get("stockMinimoSugerido")).intValue();
            var invOpt = inventarioRepository.findByMedicamentoId(medId);
            if (invOpt.isPresent()) {
                Inventario inv = invOpt.get();
                inv.setStockMinimo(sugerido);
                inventarioRepository.save(inv);
            }
            medicamentoRepository.findById(medId).ifPresent(m -> {
                m.setStockMinimo(sugerido);
                medicamentoRepository.save(m);
            });
            aplicados++;
        }
        return aplicados;
    }
}
