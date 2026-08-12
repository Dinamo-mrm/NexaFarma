package com.nexafarma.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Compra realizada a un proveedor. Responsabilidad del Desarrollador 3
 * (Transacciones, Frontend y Formulas).
 */
@Entity
@Table(name = "compras", uniqueConstraints = @UniqueConstraint(columnNames = "numero_compra"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_compra", nullable = false, length = 30)
    private String numeroCompra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_responsable_id", nullable = false)
    private Empleado empleadoResponsable;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleCompra> detalles = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal impuestos;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 25)
    private MetodoPago formaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private EstadoCompra estado = EstadoCompra.PENDIENTE;

    @PrePersist
    void alCrear() {
        if (fechaCompra == null) {
            fechaCompra = LocalDateTime.now();
        }
    }
}
