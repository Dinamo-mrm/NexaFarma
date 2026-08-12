package com.nexafarma.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro de auditoria de cada entrada, salida o ajuste de inventario.
 * Responsabilidad del Desarrollador 2 (Core Farmaceutico e Inventario).
 */
@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    private TipoMovimientoInventario tipoMovimiento;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_responsable_id", nullable = false)
    private Empleado usuarioResponsable;

    @Column(length = 255)
    private String motivo;

    @Column(name = "existencia_anterior", nullable = false)
    private Integer existenciaAnterior;

    @Column(name = "nueva_existencia", nullable = false)
    private Integer nuevaExistencia;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
