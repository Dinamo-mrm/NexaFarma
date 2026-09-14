package com.nexafarma.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datos laborales y personales del empleado. Las credenciales de acceso
 * viven en {@link Usuario} (relacion 1 a 1), separadas segun la
 * especificacion de FarmaSoft Plus.
 * Responsabilidad del Desarrollador 1 (Infraestructura y Seguridad).
 */
@Entity
@Table(name = "empleados", uniqueConstraints = @UniqueConstraint(columnNames = "documento"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String documento;

    @NotBlank
    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Email
    @Column(length = 150)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String cargo;

    @DecimalMin(value = "0.0", inclusive = true)
    @Column(precision = 12, scale = 2)
    private BigDecimal salario;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    private LocalDate fechaIngreso;

    @OneToOne(mappedBy = "empleado", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Usuario usuario;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = LocalDateTime.now();
    }
}
