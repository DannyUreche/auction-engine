package com.DannyU.auction_engine.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Auction Engine API",
                version = "1.0",
                description = "API pentru aplicatia de licitatii protejat cu JWT"
        ),
        security = @SecurityRequirement(name = "bearerAuth") // Aplica securitatea pe toate rutele
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Lipeste aici token-ul JWT generat la login (FARA cuvantul 'Bearer ', il pune el automat)",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}