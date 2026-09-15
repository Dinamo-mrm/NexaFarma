package com.nexafarma.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "domicilios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Domicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    /** Opcional: domicilio a consumidor final / sin ficha de cliente. */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @NotBlank
    @Column(name = "direccion_entrega", nullable = false, length = 200)
    private String direccionEntrega;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domiciliario_id")
    private Empleado domiciliario;

    @Column(name = "valor_domicilio", precision = 12, scale = 2)
    private BigDecimal valorDomicilio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoDomicilio estado = EstadoDomicilio.PENDIENTE;

    @Column(name = "hora_salida")
    private LocalDateTime horaSalida;

    @Column(name = "hora_entrega")
    private LocalDateTime horaEntrega;

    /** true si la venta incluye medicamentos con requiereRefrigeracion. */
    @Column(name = "requiere_transporte_termico", nullable = false)
    @Builder.Default
    private boolean requiereTransporteTermico = false;

    /** Confirmación de uso de nevera portátil (obligatoria si hay cadena de frío). */
    @Column(name = "nevera_portatil_confirmada", nullable = false)
    @Builder.Default
    private boolean neveraPortatilConfirmada = false;

    /** Monto con el que paga el cliente (efectivo). */
    @Column(name = "monto_paga_cliente", precision = 12, scale = 2)
    private BigDecimal montoPagaCliente;

    /** Cambio exacto que debe llevar el domiciliario. */
    @Column(name = "cambio_en_ruta", precision = 12, scale = 2)
    private BigDecimal cambioEnRuta;

    /** PoD: foto de guía firmada o evidencia. */
    @Column(name = "evidencia_entrega_url", length = 500)
    private String evidenciaEntregaUrl;

    /** PoD: firma digital (URL o data). */
    @Column(name = "firma_digital_url", length = 500)
    private String firmaDigitalUrl;
}
