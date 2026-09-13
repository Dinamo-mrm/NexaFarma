package com.nexafarma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/devoluciones")
public class DevolucionViewController {
    @GetMapping
    public String lista() {
        return "devoluciones/lista";
    }
}
