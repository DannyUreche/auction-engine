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

    // The API access rules, also used by OpenApiConfig to mark the public operations in Swagger
    static final String ADMIN = "/api/admin/**";
    static final String[] AUTHENTICATED_FIRST = {"/api/collections/mine", "/api/collections/containing/*", "/api/shows/eligible-items",
            "/api/items/my", "/api/items/won", "/api/items/watched", "/api/items/*/contact", "/api/items/*/auto-bid",
            "/api/items/*/messages", "/api/items/recommended", "/api/items/*/order", "/api/bids/my"};
    static final String PUBLIC_ANY = "/api/auth/**";
    static final String[] PUBLIC_GET = {"/api/images/**", "/api/items", "/api/items/**", "/api/bids/**", "/api/categories", "/api/stats",
            "/api/market", "/api/health", "/api/users/*/profile", "/api/users/*/badges", "/api/leaderboard", "/api/shows",
            "/api/shows/*", "/api/shows/*/stream", "/api/shows/*/stream/inbox", "/api/collections/*", "/api/users/*/collections", "/api/push/public-key", "/api/ai/config", "/api/events"};
    static final String[] PUBLIC_POST = {"/api/shows/*/stream/join", "/api/shows/*/stream/leave", "/api/shows/*/stream/signal", "/api/stripe/webhook", "/api/client-errors", "/api/items/*/presence", "/api/items/*/presence/leave"};

    // Same order as the filter chain below: can this API route be called without a token?
    static boolean isPublic(String method, String path) {
        org.springframework.util.AntPathMatcher matcher = new org.springframework.util.AntPathMatcher();
        java.util.function.Predicate<String[]> any = patterns -> java.util.Arrays.stream(patterns).anyMatch(p -> matcher.match(p, path));
        if (matcher.match(ADMIN, path) || any.test(AUTHENTICATED_FIRST)) {
            return false;
        }
        if (matcher.match(PUBLIC_ANY, path)) {
            return true;
        }
        return ("GET".equalsIgnoreCase(method) && any.test(PUBLIC_GET)) || ("POST".equalsIgnoreCase(method) && any.test(PUBLIC_POST));
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
                                PUBLIC_ANY,
                                // Pagina interna de erori (altfel un 404/405 apare ca 401)
                                "/error"
                        ).permitAll()
                        // Pagina de administrare: doar utilizatorii cu rolul ADMIN (vezi AdminAccess)
                        .requestMatchers(ADMIN).hasRole("ADMIN")
                        // Rutele "ale mele" cer autentificare - trebuie declarate INAINTEA celor publice de mai jos,
                        // altfel /api/items/{id} s-ar potrivi si pe /api/items/my
                        .requestMatchers(AUTHENTICATED_FIRST).authenticated()
                        // Oricine (si fara cont) poate vedea licitatiile, istoricul ofertelor, pozele, evenimentele live...
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        // ...trimite erori JavaScript (limitat per IP) si semnalul de prezenta din camera live
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        // Orice alta ruta din API necesita un token JWT valid
                        .requestMatchers("/api/**").authenticated()
                        // Restul sunt fisierele interfetei React (index.html, /assets/...) si paginile ei - publice
                        .anyRequest().permitAll()
                )
                // Fara token valid raspundem cu 401 si un mesaj in acelasi format ca restul erorilor
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"eroare\":\"Trebuie sa fii autentificat (token lipsa, invalid sau expirat)!\"}");
                        })
                        // Logat, dar fara drepturi (ex: utilizator obisnuit pe /api/admin) -> 403
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"eroare\":\"Nu ai drepturi de administrator!\"}");
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