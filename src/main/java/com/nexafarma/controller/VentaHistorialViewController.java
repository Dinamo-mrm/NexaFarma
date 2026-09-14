package com.nexafarma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador de VISTA para el historial de ventas — requisito explícito de
 * la especificación (Gestión de Ventas: "Consultar el historial de ventas" /
 * "Anular una venta con autorización del administrador"). Separado de
 * {@link VentaController} (API REST) y de {@link VentaViewController} (POS).
 *
 * Rutas:
 *   GET /ventas/historial  → vista principal del historial
 *   GET /ventas            → redirección al historial (atajo)
 */
@Controller
@RequestMapping("/ventas")
public class VentaHistorialViewController {

    @GetMapping({"/historial", ""})
    public String historial() {
        return "ventas/historial";
    }
}
