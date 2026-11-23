package com.DannyU.auction_engine.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Rutele publice (Swagger si Autentificare)
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/auth/**",
                                // Pagina interna de erori (altfel un 404/405 apare ca 401)
                                "/error"
                        ).permitAll()
                        // Imaginile obiectelor sunt publice (tag-ul <img> din browser nu trimite token-ul)
                        .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                        // Rutele "ale mele" cer autentificare - trebuie declarate INAINTEA celor publice de mai jos,
                        // altfel /api/items/{id} s-ar potrivi si pe /api/items/my
                        .requestMatchers("/api/items/my", "/api/items/won", "/api/bids/my").authenticated()
                        // Oricine (si fara cont) poate vedea licitatiile si istoricul ofertelor
                        .requestMatchers(HttpMethod.GET, "/api/items", "/api/items/**", "/api/bids/**").permitAll()
                        // Evenimentele in timp real (EventSource nu poate trimite token-ul)
                        .requestMatchers(HttpMethod.GET, "/api/events").permitAll()
                        // ORICE ALTA RUTA necesită acum un token JWT valid!
                        .anyRequest().authenticated()
                )
                // Fara token valid raspundem cu 401 si un mesaj in acelasi format ca restul erorilor
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"eroare\":\"Trebuie sa fii autentificat (token lipsa, invalid sau expirat)!\"}");
                        })
                )
                // Nu salvam sesiuni pe server, verificam token-ul la fiecare cerere
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Punem filtrul nostru JWT inaintea filtrului standard de autentificare
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}