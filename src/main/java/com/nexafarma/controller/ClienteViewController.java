package com.nexafarma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador de VISTA (server-side, Thymeleaf) para el módulo de Clientes.
 * Se mantiene separado de {@link ClienteController} (API REST) siguiendo la
 * arquitectura en capas del proyecto (... Controller, View, Security ...):
 * este solo resuelve la plantilla; la carga y guardado de datos ocurre en
 * el navegador contra /api/clientes (ver static/js/clientes.js).
 */
@Controller
@RequestMapping("/clientes")
public class ClienteViewController {

    @GetMapping
    public String lista() {
        return "clientes/lista";
    }
}
