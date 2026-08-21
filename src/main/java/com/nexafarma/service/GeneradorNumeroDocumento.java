package com.nexafarma.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Genera identificadores legibles y únicos (numero_compra / numero_venta)
 * combinando un prefijo de documento con fecha/hora y un contador de
 * milisegundos, evitando colisiones sin depender de una secuencia de BD.
 * Ej: VTA-20260821-153245-873
 */
@Component
public class GeneradorNumeroDocumento {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    public String generar(String prefijo) {
        return prefijo + "-" + LocalDateTime.now().format(FORMATO);
    }
}
