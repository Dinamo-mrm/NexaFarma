package com.nexafarma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReporteViewController {

    @GetMapping("/reportes")
    public String centroReportes() {
        return "reportes/lista";
    }
}
