package com.nexafarma.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Existencia agregada y consultable de un medicamento (suma de todos sus
 * lotes activos). Se mantiene como cache actualizado por el Service layer
 * cada vez que ocurre un {@link MovimientoInventario}, para que las
 * consultas de stock no tengan que recorrer todos los lotes.
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
