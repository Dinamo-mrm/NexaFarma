package com.nexafarma.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Existencia agregada de un medicamento (suma de lotes ACTIVO).
 * version: optimistic locking para evitar overselling entre cajas concurrentes.
 */
@Entity
@Table(name = "inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false, unique = true)
    private Medicamento medicamento;

    @Column(name = "cantidad_disponible", nullable = false)
    @Builder.Default
    private Integer cantidadDisponible = 0;

    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Integer stockMinimo = 10;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;

    /** Optimistic locking: Hibernate incrementa en cada update. */
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    public boolean stockBajo() {
        return cantidadDisponible != null && stockMinimo != null && cantidadDisponible <= stockMinimo;
    }

    public boolean agotado() {
        return cantidadDisponible == null || cantidadDisponible <= 0;
    }

    @PrePersist
    @PreUpdate
    void alGuardar() {
        ultimaActualizacion = LocalDateTime.now();
    }
}
