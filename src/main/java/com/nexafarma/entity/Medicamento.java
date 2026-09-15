package com.nexafarma.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Especialización de {@link Producto} para medicamentos.
 * aptoFraccionamiento: solo si el empaque primario trae lote y vencimiento impresos (INVIMA).
 */
@Entity
@Table(name = "medicamentos")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Medicamento extends Producto {

    @Column(name = "nombre_generico", length = 200)
    private String nombreGenerico;

    @Column(length = 100)
    private String concentracion;

    @Column(name = "laboratorio_fabricante", length = 150)
    private String laboratorioFabricante;

    @NotBlank
    @Column(name = "registro_invima", nullable = false, length = 50)
    private String registroInvima;

    @Column(name = "venta_libre", nullable = false)
    @Builder.Default
    private boolean ventaLibre = true;

    @Column(name = "requiere_formula", nullable = false)
    @Builder.Default
    private boolean requiereFormula = false;

    @Column(name = "uso_controlado", nullable = false)
    @Builder.Default
    private boolean usoControlado = false;

    @Column(name = "requiere_refrigeracion", nullable = false)
    @Builder.Default
    private boolean requiereRefrigeracion = false;

    @Column(name = "tiene_restricciones", nullable = false)
    @Builder.Default
    private boolean tieneRestricciones = false;

    /**
     * true solo si es legal fraccionar (empaque primario con lote y fecha de vencimiento).
     * Si es false, el POS/backend debe vender únicamente la presentación completa.
     */
    @Column(name = "apto_fraccionamiento", nullable = false)
    @Builder.Default
    private boolean aptoFraccionamiento = false;
}
