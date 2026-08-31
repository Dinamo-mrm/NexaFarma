package com.nexafarma.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexafarma.entity.Categoria;
import com.nexafarma.entity.Cliente;
import com.nexafarma.entity.DetalleFormula;
import com.nexafarma.entity.Empleado;
import com.nexafarma.entity.EstadoLote;
import com.nexafarma.entity.EstadoVenta;
import com.nexafarma.entity.FormulaMedica;
import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Lote;
import com.nexafarma.entity.Medicamento;
import com.nexafarma.entity.Proveedor;
import com.nexafarma.entity.Venta;
import com.nexafarma.repository.CategoriaRepository;
import com.nexafarma.repository.ClienteRepository;
import com.nexafarma.repository.EmpleadoRepository;
import com.nexafarma.repository.FormulaMedicaRepository;
import com.nexafarma.repository.InventarioRepository;
import com.nexafarma.repository.LoteRepository;
import com.nexafarma.repository.MedicamentoRepository;
import com.nexafarma.repository.ProveedorRepository;
import com.nexafarma.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre el flujo de venta de punta a punta (punto de venta) contra la API
 * REST real: FEFO en el consumo de lotes, bloqueo de lote vencido, exigencia
 * automática de fórmula médica vigente para medicamentos con
 * {@code requiereFormula}, y anulación con reversión de stock.
 * Responsabilidad del Desarrollador 3.
 *
 * <p>Los controladores devuelven las entidades JPA directamente (sin DTOs);
 * las asociaciones bidireccionales usan {@code @JsonManagedReference}/
 * {@code @JsonBackReference} y las consultas de detalle usan fetch join, por
 * eso el JSON de una {@code Venta} trae {@code detalles[].medicamento} y
 * {@code detalles[].lote} completos en lugar de ids sueltos.</p>
 *
 * <p>La clase se anota con {@code @Transactional} para que cada test
 * arranque con la base de datos limpia (rollback automático al terminar).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Spring Security aun no valida roles (pendiente Dev1)
@ActiveProfiles("test")
@Transactional
class VentaFlujoCompletoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private EmpleadoRepository empleadoRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private MedicamentoRepository medicamentoRepository;
    @Autowired
    private LoteRepository loteRepository;
    @Autowired
    private InventarioRepository inventarioRepository;
    @Autowired
    private FormulaMedicaRepository formulaMedicaRepository;
    @Autowired
    private VentaRepository ventaRepository;

    private Cliente cliente;
    private Empleado empleado;
    private Medicamento medicamentoVentaLibre;
    private Medicamento medicamentoControlado;

    @BeforeEach
    void setUp() {
        Proveedor proveedor = proveedorRepository.save(Proveedor.builder()
                .nit("900123456-1").razonSocial("Distribuidora Salud SAS").activo(true).build());
        Categoria categoria = categoriaRepository.save(Categoria.builder()
                .nombre("Analgesicos-" + System.nanoTime()).descripcion("Medicamentos para el dolor").build());

        cliente = clienteRepository.save(Cliente.builder()
                .documento("1094123456").nombreCompleto("Ana Maria Rios")
                .correo("ana.rios@example.com").activo(true).build());
        empleado = empleadoRepository.save(Empleado.builder()
                .documento("1094999999").nombreCompleto("Carlos Vendedor")
                .cargo("Vendedor").activo(true).build());

        medicamentoVentaLibre = medicamentoRepository.save(Medicamento.builder()
                .codigoInterno("MED-001").codigoBarras("7701234560001")
                .nombreComercial("Acetaminofen 500mg").categoria(categoria).proveedor(proveedor)
                .precioCompra(new BigDecimal("1000")).precioVenta(new BigDecimal("2000"))
                .stockMinimo(5).registroInvima("INVIMA-2024-M-001")
                .ventaLibre(true).requiereFormula(false).build());
        inventarioRepository.save(Inventario.builder()
                .medicamento(medicamentoVentaLibre).cantidadDisponible(0).stockMinimo(5).build());

        medicamentoControlado = medicamentoRepository.save(Medicamento.builder()
                .codigoInterno("MED-002").codigoBarras("7701234560002")
                .nombreComercial("Tramadol 50mg").categoria(categoria).proveedor(proveedor)
                .precioCompra(new BigDecimal("3000")).precioVenta(new BigDecimal("6000"))
                .stockMinimo(5).registroInvima("INVIMA-2024-M-002")
                .ventaLibre(false).requiereFormula(true).usoControlado(true).build());
        inventarioRepository.save(Inventario.builder()
                .medicamento(medicamentoControlado).cantidadDisponible(0).stockMinimo(5).build());
    }

    private Lote crearLote(Medicamento medicamento, String numero, LocalDate vencimiento, int cantidad, EstadoLote estado) {
        Lote lote = loteRepository.save(Lote.builder()
                .medicamento(medicamento).numeroLote(numero)
                .fechaFabricacion(LocalDate.now().minusMonths(6)).fechaVencimiento(vencimiento)
                .cantidadDisponible(cantidad).estado(estado).build());
        Inventario inventario = inventarioRepository.findByMedicamentoId(medicamento.getId()).orElseThrow();
        inventario.setCantidadDisponible(inventario.getCantidadDisponible() + cantidad);
        inventarioRepository.save(inventario);
        return lote;
    }

    private String bodyVenta(Long clienteId, Long empleadoId, Long medicamentoId, int cantidad) {
        return """
                {
                  "cliente": {"id": %d},
                  "empleado": {"id": %d},
                  "metodoPago": "EFECTIVO",
                  "detalles": [ { "medicamento": {"id": %d}, "cantidad": %d } ]
                }
                """.formatted(clienteId, empleadoId, medicamentoId, cantidad);
    }

    @Test
    void registrarVenta_aplicaFefo_yActualizaInventarioAgregado() throws Exception {
        Lote loteProximoAVencer = crearLote(medicamentoVentaLibre, "L-A", LocalDate.now().plusDays(30), 3, EstadoLote.ACTIVO);
        Lote loteMasVigente = crearLote(medicamentoVentaLibre, "L-B", LocalDate.now().plusDays(180), 10, EstadoLote.ACTIVO);

        mockMvc.perform(post("/api/ventas").contentType("application/json")
                        .content(bodyVenta(cliente.getId(), empleado.getId(), medicamentoVentaLibre.getId(), 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.total").value(10000.0))
                .andExpect(jsonPath("$.detalles.length()").value(2))
                .andExpect(jsonPath("$.detalles[0].lote.numeroLote").value("L-A"))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(3))
                .andExpect(jsonPath("$.detalles[1].lote.numeroLote").value("L-B"))
                .andExpect(jsonPath("$.detalles[1].cantidad").value(2));

        Lote loteAActualizado = loteRepository.findById(loteProximoAVencer.getId()).orElseThrow();
        Lote loteBActualizado = loteRepository.findById(loteMasVigente.getId()).orElseThrow();
        assertThat(loteAActualizado.getCantidadDisponible()).isZero();
        assertThat(loteBActualizado.getCantidadDisponible()).isEqualTo(8);

        Inventario inventario = inventarioRepository.findByMedicamentoId(medicamentoVentaLibre.getId()).orElseThrow();
        assertThat(inventario.getCantidadDisponible()).isEqualTo(8);
    }

    @Test
    void registrarVenta_loteVencido_seExcluyeDelFefoYFallaPorStockInsuficiente() throws Exception {
        crearLote(medicamentoVentaLibre, "L-VENCIDO", LocalDate.now().minusDays(5), 20, EstadoLote.VENCIDO);

        mockMvc.perform(post("/api/ventas").contentType("application/json")
                        .content(bodyVenta(cliente.getId(), empleado.getId(), medicamentoVentaLibre.getId(), 1)))
                .andExpect(status().isBadRequest());

        Inventario inventario = inventarioRepository.findByMedicamentoId(medicamentoVentaLibre.getId()).orElseThrow();
        assertThat(inventario.getCantidadDisponible()).isZero();
    }

    @Test
    void registrarVenta_medicamentoControlado_sinFormula_esRechazada() throws Exception {
        crearLote(medicamentoControlado, "L-CTRL", LocalDate.now().plusDays(90), 10, EstadoLote.ACTIVO);

        mockMvc.perform(post("/api/ventas").contentType("application/json")
                        .content(bodyVenta(cliente.getId(), empleado.getId(), medicamentoControlado.getId(), 1)))
                .andExpect(status().isBadRequest());

        Lote lote = loteRepository.findByMedicamentoIdAndEstadoOrderByFechaVencimientoAsc(
                medicamentoControlado.getId(), EstadoLote.ACTIVO).get(0);
        assertThat(lote.getCantidadDisponible()).isEqualTo(10);
    }

    @Test
    void registrarVenta_medicamentoControlado_conFormulaVigenteDelCliente_seVendeCorrectamente() throws Exception {
        crearLote(medicamentoControlado, "L-CTRL", LocalDate.now().plusDays(90), 10, EstadoLote.ACTIVO);

        FormulaMedica formula = FormulaMedica.builder()
                .cliente(cliente).nombreMedico("Dr. Julian Perez")
                .numeroTarjetaProfesional("TP-11223").fechaExpedicion(LocalDate.now())
                .duracionTratamientoDias(15).build();
        DetalleFormula detalle = DetalleFormula.builder()
                .formulaMedica(formula).medicamento(medicamentoControlado)
                .dosis("50mg").frecuencia("Cada 8 horas").duracionTratamiento("5 dias").build();
        formula.getDetalles().add(detalle);
        formulaMedicaRepository.save(formula);

        mockMvc.perform(post("/api/ventas").contentType("application/json")
                        .content(bodyVenta(cliente.getId(), empleado.getId(), medicamentoControlado.getId(), 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.total").value(12000.0));
    }

    @Test
    void anularVenta_revierteStockDeLoteEInventarioAgregado() throws Exception {
        crearLote(medicamentoVentaLibre, "L-A", LocalDate.now().plusDays(60), 10, EstadoLote.ACTIVO);

        String respuesta = mockMvc.perform(post("/api/ventas").contentType("application/json")
                        .content(bodyVenta(cliente.getId(), empleado.getId(), medicamentoVentaLibre.getId(), 4)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Venta ventaCreada = objectMapper.readValue(respuesta, Venta.class);

        Inventario inventarioTrasVenta = inventarioRepository.findByMedicamentoId(medicamentoVentaLibre.getId()).orElseThrow();
        assertThat(inventarioTrasVenta.getCantidadDisponible()).isEqualTo(6);

        mockMvc.perform(patch("/api/ventas/" + ventaCreada.getId() + "/anular"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/ventas/" + ventaCreada.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));

        Inventario inventarioTrasAnular = inventarioRepository.findByMedicamentoId(medicamentoVentaLibre.getId()).orElseThrow();
        assertThat(inventarioTrasAnular.getCantidadDisponible()).isEqualTo(10);

        Lote lote = loteRepository.findByMedicamentoIdAndEstadoOrderByFechaVencimientoAsc(
                medicamentoVentaLibre.getId(), EstadoLote.ACTIVO).get(0);
        assertThat(lote.getCantidadDisponible()).isEqualTo(10);
        assertThat(ventaRepository.findById(ventaCreada.getId()).orElseThrow().getEstado()).isEqualTo(EstadoVenta.ANULADA);
    }
}
