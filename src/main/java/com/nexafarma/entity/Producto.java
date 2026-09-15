package com.nexafarma.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Superclase comun a cualquier articulo comercializable por la farmacia
 * (medicamentos y tambien productos de cuidado personal, dermatologicos,
 * para bebes, dispositivos medicos, vitaminas, suplementos, higiene, etc.).
 *
 * {@link Medicamento} extiende esta clase agregando los campos exclusivos
 * del sector farmaceutico (registro INVIMA, control, refrigeracion...).
 * Se usa estrategia JOINED para mantener la integridad relacional y permitir
 * futuros tipos de producto sin duplicar columnas.
 */
@Entity
@Table(name = "productos")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "codigo_interno", nullable = false, unique = true, length = 30)
    private String codigoInterno;

    @NotBlank
    @Column(name = "codigo_barras", nullable = false, unique = true, length = 50)
    private String codigoBarras;

    @NotBlank
    @Column(name = "nombre_comercial", nullable = false, length = 200)
    private String nombreComercial;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 80)
    private String presentacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "precio_compra", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCompra;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Integer stockMinimo = 10;

    @Column(length = 100)
    private String ubicacion;

    /** Puntos de fidelización otorgados por unidad vendida. */
    @Column(name = "puntos_por_unidad", nullable = false)
    @Builder.Default
    private Integer puntosPorUnidad = 0;

    /**
     * Unidades mínimas por presentación de venta (ej. 1 caja = 30 tabletas → factor 30).
     * El inventario se maneja siempre en unidad mínima.
     */
    @Column(name = "factor_conversion", nullable = false)
    @Builder.Default
    private Integer factorConversion = 1;

    /**
     * Unidades mínimas por blíster (ej. blíster x 10 tabletas → 10).
     * Debe ser divisor de factorConversion cuando ambos &gt; 1.
     */
    @Column(name = "factor_blister", nullable = false)
    @Builder.Default
    private Integer factorBlister = 1;

    /** Nombre de la unidad mínima (TABLETA, ML, UNIDAD...). */
    @Column(name = "unidad_minima", length = 40)
    @Builder.Default
    private String unidadMinima = "UNIDAD";

    /**
     * Precio máximo de venta regulado (CNPMDM). Null = sin techo regulado.
     * El precio de venta no puede superar este valor.
     */
    @Column(name = "precio_maximo_regulado", precision = 12, scale = 2)
    private java.math.BigDecimal precioMaximoRegulado;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}


