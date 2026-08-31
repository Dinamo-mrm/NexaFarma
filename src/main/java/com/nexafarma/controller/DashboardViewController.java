package com.nexafarma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de VISTA (server-side, Thymeleaf) para la página de inicio.
 * Mapeado a "/" para que la interfaz arranque directamente en el Panel de
 * control, sin pantalla de login (pendiente de Dev1). Los datos se cargan
 * en el navegador contra /api/ventas, /api/clientes y /api/alertas
 * (ver static/js/dashboard.js).
 */
@Controller
public class DashboardViewController {

    @GetMapping("/")
    public String panelDeControl() {
        return "dashboard";
    }
}
