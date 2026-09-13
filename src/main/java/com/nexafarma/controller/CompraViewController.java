package com.nexafarma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/compras")
public class CompraViewController {
    @GetMapping
    public String lista() {
        return "compras/lista";
    }
}
