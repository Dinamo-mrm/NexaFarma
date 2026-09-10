package com.nexafarma.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Muestra la pantalla personalizada de inicio de sesion. */
@Controller
public class LoginViewController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
