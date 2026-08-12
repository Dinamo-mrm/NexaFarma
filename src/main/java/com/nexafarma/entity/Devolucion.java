package com.nexafarma.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Devolucion de un cliente o hacia un proveedor. Requiere validacion del
 * farmaceutico antes de reingresar el producto al inventario disponible.
 */
@Entity
@Table(name = "devoluciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoDevolucion tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_origen_id")
    private Venta ventaOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_origen_id")
    private Compra compraOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Min(1)
    @Column(nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotivoDevolucion motivo;

    @Column(name = "estado_producto", length = 100)
    private String estadoProducto;

    /** El sistema no debe reingresar stock automaticamente sin esta validacion. */
    @Column(name = "validado_por_farmaceutico", nullable = false)
    @Builder.Default
    private boolean validadoPorFarmaceutico = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validado_por_id")
    private Empleado validadoPor;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
