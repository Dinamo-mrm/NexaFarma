package com.nexafarma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador de VISTA (server-side, Thymeleaf) para el Punto de Venta.
 * Separado de {@link VentaController} (API REST); ver static/js/ventas-pos.js.
 */
@Controller
@RequestMapping("/ventas")
public class VentaViewController {

    @GetMapping("/pos")
    public String puntoDeVenta() {
        return "ventas/pos";
    }
}
