package com.nexafarma.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cliente de la farmacia. Modulo a cargo del Desarrollador 3
 * (Transacciones, Frontend y Formulas).
 */
@Entity
@Table(name = "clientes", uniqueConstraints = @UniqueConstraint(columnNames = "documento"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String documento;

    @NotBlank
    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(length = 20)
    private String telefono;

    @Email
    @Column(length = 150)
    private String correo;

    @Column(length = 200)
    private String direccion;

    @Past
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /** Opcional: Entidad Promotora de Salud. */
    @Column(length = 100)
    private String eps;

    /** Opcional: alergias conocidas, relevante para validar formulas medicas. */
    @Column(length = 500)
    private String alergias;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

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
