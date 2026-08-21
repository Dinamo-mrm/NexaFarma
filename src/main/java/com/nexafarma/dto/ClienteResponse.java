package com.nexafarma.dto;

import com.nexafarma.entity.Cliente;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String documento,
        String nombreCompleto,
        String telefono,
        String correo,
        String direccion,
        LocalDate fechaNacimiento,
        String eps,
        String alergias,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
    public static ClienteResponse desde(Cliente c) {
        return new ClienteResponse(
                c.getId(), c.getDocumento(), c.getNombreCompleto(), c.getTelefono(),
                c.getCorreo(), c.getDireccion(), c.getFechaNacimiento(), c.getEps(),
                c.getAlergias(), c.isActivo(), c.getCreadoEn(), c.getActualizadoEn()
        );
    }
}
