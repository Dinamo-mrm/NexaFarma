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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cliente de la farmacia.
 * Soft delete vía activo=false. Habeas Data (Ley 1581): autorizaTratamientoDatos.
 * Campos DIAN: tipoDocumento, documento, emailFacturacion, responsabilidadTributaria.
 */
@Entity
@Table(name = "clientes", uniqueConstraints = @UniqueConstraint(columnNames = "documento"))
@SQLDelete(sql = "UPDATE clientes SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CC, NIT, CE, PASAPORTE, TI (exigencia DIAN / facturación electrónica). */
    @Column(name = "tipo_documento", nullable = false, length = 10)
    @Builder.Default
    private String tipoDocumento = "CC";

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

    /** Correo para facturación electrónica si difiere del de contacto. */
    @Email
    @Column(name = "email_facturacion", length = 150)
    private String emailFacturacion;

    /** Ej: Responsable de IVA, No responsable de IVA, Régimen simple… */
    @Column(name = "responsabilidad_tributaria", length = 80)
    private String responsabilidadTributaria;

    @Column(length = 200)
    private String direccion;

    @Past
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 100)
    private String eps;

    @Column(length = 500)
    private String alergias;

    /**
     * Consentimiento explícito de tratamiento de datos personales (Ley 1581 de 2012).
     */
    @Column(name = "autoriza_tratamiento_datos", nullable = false)
    @Builder.Default
    private boolean autorizaTratamientoDatos = false;

    @Column(name = "puntos_fidelizacion", nullable = false)
    @Builder.Default
    private int puntosFidelizacion = 0;

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
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            tipoDocumento = "CC";
        }
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = LocalDateTime.now();
    }

    public boolean puedeRecibirComunicaciones() {
        return activo && autorizaTratamientoDatos;
    }
}
