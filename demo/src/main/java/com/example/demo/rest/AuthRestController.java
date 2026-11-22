package com.example.demo.rest;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.Credenciales;
import com.example.demo.entities.Rol;
import com.example.demo.entities.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final EdicionUsuario edicionUsuario;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthRestController(UsuarioService usuarioService, UsuarioRepository usuarioRepository,
                              EdicionUsuario edicionUsuario, AuthenticationManager authenticationManager) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.edicionUsuario = edicionUsuario;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credenciales credenciales,
                                   HttpServletRequest request, HttpServletResponse response) {
        Authentication autenticacion;
        try {
            // Spring Security busca el usuario (UsuarioDetailsService) y compara con BCrypt
            autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(credenciales.correo(), credenciales.password()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Correo o contraseña incorrectos."));
        }

        // Nuevo id de sesión tras el login (evita fijación de sesión)
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        // Guarda la autenticación en la sesión HTTP (cookie JSESSIONID)
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);

        Usuario usuario = usuarioRepository.findByCorreo(autenticacion.getName()).orElseThrow();
        request.getSession().setAttribute("usuario", usuario);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/sesion")
    public ResponseEntity<Usuario> sesion(HttpSession sesion) {
        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/registro")
    public ResponseEntity<Usuario> registro(@RequestBody Usuario datos) {
        Usuario usuario = new Usuario();
        datos.setRol(Rol.CLIENTE);
        datos.setBarberia(null);
        datos.setActivo(true);
        edicionUsuario.aplicar(usuario, datos);
        usuarioService.registrarCliente(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    // El cierre de sesión (POST /api/auth/logout) lo maneja Spring Security en SecurityConfig
}
