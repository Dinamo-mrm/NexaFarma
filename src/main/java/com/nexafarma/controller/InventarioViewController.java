package com.nexafarma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/inventario")
public class InventarioViewController {
    @GetMapping
    public String lista() {
        return "inventario/lista";
    }
}
