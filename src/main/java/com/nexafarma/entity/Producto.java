package com.nexafarma.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    private Integer stockMinimo = 10;

    @Column(length = 100)
    private String ubicacion;

    @Column(nullable = false)
    private boolean activo = true;
}
