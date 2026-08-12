package com.nexafarma.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pedido a domicilio asociado a una venta. Responsabilidad del
 * Desarrollador 3 (modulo avanzado, Fase 3).
 */
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
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
}
