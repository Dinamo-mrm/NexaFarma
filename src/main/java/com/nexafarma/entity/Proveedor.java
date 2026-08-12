package com.nexafarma.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Proveedor de medicamentos. Modulo a cargo del Desarrollador 2
 * (Core Farmaceutico e Inventario).
 */
@Entity
@Table(name = "proveedores", uniqueConstraints = @UniqueConstraint(columnNames = "nit"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String nit;

    @NotBlank
    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "nombre_contacto", length = 150)
    private String nombreContacto;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String correo;

    @Column(length = 200)
    private String direccion;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
