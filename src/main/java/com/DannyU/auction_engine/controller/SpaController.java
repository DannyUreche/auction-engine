package com.DannyU.auction_engine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// In productie interfata React este servita de Spring Boot (din static/index.html).
// Paginile React (ex: /items/5) nu exista pe server, asa ca le trimitem tot la index.html,
// iar React Router afiseaza pagina potrivita. Fara asta, un refresh pe /items/5 ar da 404.
@Controller
public class SpaController {

    @GetMapping({"/login", "/register", "/forgot-password", "/reset-password", "/sell", "/account", "/admin", "/items/{id}", "/users/{username}"})
    public String forwardToReact() {
        return "forward:/index.html";
    }
}
