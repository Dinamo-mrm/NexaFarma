package com.nexafarma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador de VISTA (server-side, Thymeleaf) para Fórmulas Médicas.
 * Separado de {@link FormulaMedicaController} (API REST); ver
 * static/js/formulas-medicas.js.
 */
@Controller
@RequestMapping("/formulas-medicas")
public class FormulaMedicaViewController {

    @GetMapping
    public String lista() {
        return "formulas/lista";
    }
}
