package com.example.demo;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.config.SecurityConfig;
import com.example.demo.config.UsuarioDetailsService;
import com.example.demo.entities.Barberia;
import com.example.demo.entities.Rol;
import com.example.demo.entities.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.rest.AuthRestController;
import com.example.demo.rest.BarberiaRestController;
import com.example.demo.rest.EdicionUsuario;
import com.example.demo.rest.PerfilRestController;
import com.example.demo.rest.UsuarioRestController;
import com.example.demo.service.BarberiaService;
import com.example.demo.service.UsuarioService;

/**
 * Pruebas de seguridad (Spring Security + BCrypt) sin base de datos:
 * los repositorios y servicios se reemplazan por mocks.
 */
@WebMvcTest(controllers = { AuthRestController.class, BarberiaRestController.class, UsuarioRestController.class,
        PerfilRestController.class })
@Import({ SecurityConfig.class, UsuarioDetailsService.class })
class SeguridadTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UsuarioRepository usuarioRepository;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private BarberiaService barberiaService;
    @MockBean
    private EdicionUsuario edicionUsuario;

    @BeforeEach
    void usuarioDePrueba() {
        Usuario cliente = new Usuario();
        cliente.setNombre("Valentina Ruiz");
        cliente.setCorreo("valentina@gmail.com");
        cliente.setPassword(passwordEncoder.encode("cliente123")); // guardada con BCrypt
        cliente.setRol(Rol.CLIENTE);
        cliente.setActivo(true);
        when(usuarioRepository.findByCorreo("valentina@gmail.com")).thenReturn(Optional.of(cliente));
    }

    @Test
    @DisplayName("1. Endpoint público: se listan barberías sin iniciar sesión")
    void accesoEndpointPublico() throws Exception {
        Barberia barberia = new Barberia();
        barberia.setNombre("Barberia la 45");
        when(barberiaService.obtenerTodas()).thenReturn(List.of(barberia));

        mvc.perform(get("/api/barberias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Barberia la 45"));
    }

    @Test
    @DisplayName("2. Inicio de sesión correcto")
    void loginCorrecto() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"valentina@gmail.com\",\"password\":\"cliente123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("valentina@gmail.com"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("3. Inicio de sesión con credenciales incorrectas")
    void loginIncorrecto() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"valentina@gmail.com\",\"password\":\"claveMala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
    }

    @Test
    @DisplayName("4. Acceso denegado: un CLIENTE intenta gestionar usuarios (solo ADMINISTRADOR)")
    void accesoDenegadoPorRol() throws Exception {
        mvc.perform(get("/api/admin/usuarios").with(user("valentina@gmail.com").roles("CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tienes permisos para acceder a esa sección."));
    }

    @Test
    @DisplayName("5. Login del administrador configurado en memoria (InMemoryUserDetailsManager)")
    void loginAdminEnMemoria() throws Exception {
        Usuario admin = new Usuario();
        admin.setCorreo("admin@gmail.com");
        admin.setRol(Rol.ADMINISTRADOR);
        when(usuarioRepository.findByCorreo("admin@gmail.com")).thenReturn(Optional.of(admin));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@gmail.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("6. Endpoint compartido /api/perfil: USER y ADMIN entran, otro rol no")
    void endpointCompartido() throws Exception {
        mvc.perform(get("/api/perfil").with(user("valentina@gmail.com").roles("CLIENTE")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/perfil").with(user("admin@gmail.com").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/perfil").with(user("andres@gmail.com").roles("BARBERO")))
                .andExpect(status().isForbidden());
    }
}
