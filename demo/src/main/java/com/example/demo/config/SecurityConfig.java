package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS usa la configuración de WebConfig (localhost:4200 con credenciales)
                .cors(Customizer.withDefaults())
                // API REST consumida por Angular con cookie de sesión
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Públicos: login, registro, sesión
                        .requestMatchers("/api/auth/**").permitAll()
                        // Compartido: USER (CLIENTE) y ADMIN (ADMINISTRADOR)
                        .requestMatchers("/api/perfil").hasAnyRole("CLIENTE", "ADMINISTRADOR")
                        // Por rol
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/gerente/**").hasRole("GERENTE")
                        .requestMatchers("/api/barbero/**").hasRole("BARBERO")
                        .requestMatchers("/api/cliente/**").hasRole("CLIENTE")
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        // 401: no hay sesión iniciada
                        .authenticationEntryPoint((request, response, e) ->
                                responder(response, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión."))
                        // 403: sesión iniciada pero sin el rol necesario
                        .accessDeniedHandler((request, response, e) ->
                                responder(response, HttpStatus.FORBIDDEN,
                                        "No tienes permisos para acceder a esa sección.")))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, auth) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Usuarios de prueba en memoria (obligatorio): 2 usuarios normales y 1 administrador.
     * Usan los mismos correos de usuarios que existen en la base de datos para que
     * sus reservas y pagos sigan funcionando en la aplicación.
     */
    @Bean
    public InMemoryUserDetailsManager usuariosEnMemoria(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("valentina@gmail.com")
                        .password(passwordEncoder.encode("cliente123"))
                        .roles("CLIENTE")
                        .build(),
                User.withUsername("mateo@gmail.com")
                        .password(passwordEncoder.encode("cliente123"))
                        .roles("CLIENTE")
                        .build(),
                User.withUsername("admin@gmail.com")
                        .password(passwordEncoder.encode("admin123"))
                        .roles("ADMINISTRADOR")
                        .build());
    }

    /**
     * El login revisa primero los usuarios en memoria y, si no está ahí,
     * los usuarios guardados en PostgreSQL (UsuarioDetailsService, opcional).
     */
    @Bean
    public AuthenticationManager authenticationManager(InMemoryUserDetailsManager usuariosEnMemoria,
                                                       UsuarioDetailsService usuariosBaseDatos,
                                                       PasswordEncoder passwordEncoder) {
        return new ProviderManager(
                proveedor(usuariosEnMemoria, passwordEncoder),
                proveedor(usuariosBaseDatos, passwordEncoder));
    }

    private static DaoAuthenticationProvider proveedor(UserDetailsService usuarios, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider();
        proveedor.setUserDetailsService(usuarios);
        proveedor.setPasswordEncoder(passwordEncoder);
        return proveedor;
    }

    private static void responder(HttpServletResponse response, HttpStatus status, String mensaje)
            throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
