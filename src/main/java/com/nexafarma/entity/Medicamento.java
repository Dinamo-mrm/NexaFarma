package com.nexafarma.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Especializacion de {@link Producto} para medicamentos: agrega los datos
 * sensibles propios del sector farmaceutico (registro sanitario INVIMA,
 * si requiere formula medica, refrigeracion, nombre generico, concentracion
 * y laboratorio fabricante).
 *
 * Responsabilidad del Desarrollador 2 (Core Farmaceutico e Inventario).
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

    /** Registro sanitario expedido por INVIMA. */
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
}
