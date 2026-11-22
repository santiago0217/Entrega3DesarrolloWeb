package com.example.demo.rest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.entities.Usuario;
import com.example.demo.exception.CorreoDuplicadoException;
import com.example.demo.service.UsuarioService;

@Component
public class EdicionUsuario {

    private final UsuarioService usuarioService;
    private final Validacion validacion;
    private final PasswordEncoder passwordEncoder;

    public EdicionUsuario(UsuarioService usuarioService, Validacion validacion, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.validacion = validacion;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario aplicar(Usuario destino, Usuario datos) {
        if (datos.getCorreo() != null && !datos.getCorreo().equals(destino.getCorreo())
                && usuarioService.existeCorreo(datos.getCorreo())) {
            throw new CorreoDuplicadoException("Ya existe una cuenta registrada con ese correo.");
        }
        destino.setNombre(datos.getNombre());
        destino.setCorreo(datos.getCorreo());
        destino.setTelefono(datos.getTelefono());
        destino.setRol(datos.getRol());
        destino.setBarberia(datos.getBarberia());
        if (datos.getActivo() != null) {
            destino.setActivo(datos.getActivo());
        }
        boolean passwordNueva = datos.getPassword() != null && !datos.getPassword().isBlank();
        if (passwordNueva) {
            destino.setPassword(datos.getPassword());
        }
        // Se valida la contraseña en texto plano (mínimo 4 caracteres) y luego se guarda con BCrypt
        validacion.validar(destino);
        if (passwordNueva) {
            destino.setPassword(passwordEncoder.encode(datos.getPassword()));
        }
        return destino;
    }
}
