package com.example.demo.config;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.entities.Usuario;
import com.example.demo.repository.UsuarioRepository;

/**
 * Carga el usuario desde la base de datos para que Spring Security
 * compare la contraseña (BCrypt) y asigne el rol como ROLE_<ROL>.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repo;

    public UsuarioDetailsService(UsuarioRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = repo.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPassword())
                .roles(usuario.getRol().name())
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }
}
