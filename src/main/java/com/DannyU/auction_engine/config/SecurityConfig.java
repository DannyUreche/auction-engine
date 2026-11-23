package com.DannyU.auction_engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Aceasta metoda ne ofera un criptator profesional pentru parole (nu le salvam niciodata in text clar!)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Configuram regulile de acces (ce este public si ce necesita autentificare)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Dezactivam CSRF pentru ca construim un API REST (folosim JSON, nu formulare HTML clasice)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Lasam libere rutele pentru Swagger si cele de autentificare pe care le vom crea imediat
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/auth/**"
                        ).permitAll()
                        // Orice alta ruta (cum ar fi plasarea unei oferte) va fi protejata in curand
                        // Momentan le lasam deschise pana terminam login-ul, ca sa nu te blochez
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}