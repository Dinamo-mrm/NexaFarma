package com.nexafarma.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/domicilios")
public class DomicilioViewController {
    @GetMapping
    public String lista() {
        return "domicilios/lista";
    }
}
