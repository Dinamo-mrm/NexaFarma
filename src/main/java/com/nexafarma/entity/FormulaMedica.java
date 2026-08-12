package com.nexafarma.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Formula medica que ampara la venta de medicamentos controlados o de
 * prescripcion. Responsabilidad del Desarrollador 3.
 */
@Entity
@Table(name = "formulas_medicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormulaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @NotBlank
    @Column(name = "nombre_medico", nullable = false, length = 150)
    private String nombreMedico;

    @NotBlank
    @Column(name = "numero_tarjeta_profesional", nullable = false, length = 50)
    private String numeroTarjetaProfesional;

    @Column(name = "entidad_salud", length = 150)
    private String entidadSalud;

    @Column(name = "fecha_expedicion", nullable = false)
    private LocalDate fechaExpedicion;

    /** Duracion en dias del tratamiento; a partir de fechaExpedicion define la vigencia. */
    @Column(name = "duracion_tratamiento_dias")
    private Integer duracionTratamientoDias;

    /** Ruta/URL del archivo o imagen adjunta de la formula escaneada. */
    @Column(name = "archivo_adjunto_url", length = 500)
    private String archivoAdjuntoUrl;

    @OneToMany(mappedBy = "formulaMedica", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleFormula> detalles = new ArrayList<>();

    /** Regla de negocio central: sin fecha de vigencia o vencida, no ampara la venta. */
    public boolean esVigente() {
        if (fechaExpedicion == null || duracionTratamientoDias == null) {
            return false;
        }
        return !fechaExpedicion.plusDays(duracionTratamientoDias).isBefore(LocalDate.now());
    }
}
