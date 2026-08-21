package com.nexafarma.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record ClienteRequest(
        @NotBlank(message = "El documento es obligatorio") String documento,
        @NotBlank(message = "El nombre completo es obligatorio") String nombreCompleto,
        String telefono,
        @Email(message = "El correo no tiene un formato válido") String correo,
        String direccion,
        @Past(message = "La fecha de nacimiento debe ser en el pasado") LocalDate fechaNacimiento,
        String eps,
        String alergias
) {
}
