package com.nexafarma.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Lote de un medicamento. Un medicamento puede tener varios lotes con
 * distintas fechas de vencimiento y cantidades. Responsabilidad del
 * Desarrollador 2 (Core Farmaceutico e Inventario).
 */
@Entity
@Table(name = "lotes", uniqueConstraints = @UniqueConstraint(columnNames = {"medicamento_id", "numero_lote"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @NotBlank
    @Column(name = "numero_lote", nullable = false, length = 50)
    private String numeroLote;

    @Column(name = "fecha_fabricacion", nullable = false)
    private LocalDate fechaFabricacion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Min(0)
    @Column(name = "cantidad_disponible", nullable = false)
    private Integer cantidadDisponible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoLote estado = EstadoLote.ACTIVO;

    /** true si el lote ya fue devuelto al proveedor. */
    @Column(name = "devuelto_a_proveedor", nullable = false)
    @Builder.Default
    private boolean devueltoAProveedor = false;

    /** Regla de negocio central: un lote vencido nunca puede venderse. */
    public boolean estaVencido() {
        return fechaVencimiento != null && fechaVencimiento.isBefore(LocalDate.now());
    }
}
