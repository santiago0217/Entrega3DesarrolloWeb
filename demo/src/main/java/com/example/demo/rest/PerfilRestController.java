package com.example.demo.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entities.Usuario;

import jakarta.servlet.http.HttpSession;

/**
 * Endpoint compartido: lo pueden usar CLIENTE (USER) y ADMINISTRADOR (ADMIN).
 * La regla de acceso está en SecurityConfig.
 */
@RestController
@RequestMapping("/api/perfil")
public class PerfilRestController {

    @GetMapping
    public Usuario miPerfil(HttpSession sesion) {
        return (Usuario) sesion.getAttribute("usuario");
    }
}
