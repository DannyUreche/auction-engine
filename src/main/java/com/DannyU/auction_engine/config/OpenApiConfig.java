package com.DannyU.auction_engine.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// Swagger / OpenAPI documentation (/swagger-ui.html, /v3/api-docs). Every operation has a summary and a tag
// (annotations on the controllers); this class adds the overview, the JWT scheme, marks the operations that
// need no token (same rules as SecurityConfig) and documents the common error answers {"eroare": "..."}.
@Configuration
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";
    private static final String ERROR_SCHEMA = "Eroare";

    private static final String DESCRIPTION = """
            API-ul platformei de licitatii **Licitatii.live** (Spring Boot 4, PostgreSQL). Interfata React foloseste exact aceste rute.

            ### Autentificare
            1. `POST /api/auth/register`, apoi `POST /api/auth/login` -> `{"token": "..."}`. Daca contul are autentificarea in doi pasi,
               login-ul intoarce `{"twoFactorRequired": true, "twoFactorTicket": "..."}` si tokenul vine din `POST /api/auth/login/2fa`.
            2. Apasa **Authorize** si lipeste tokenul (fara `Bearer `). Tokenul e valabil 24 de ore.

            Operatiile fara lacat sunt publice. Utilizatorul curent vine mereu din token - nicio ruta nu primeste id-ul lui in corp.

            ### Erori
            Toate erorile au forma `{"eroare": "mesaj in romana"}`: **400** regula incalcata sau date invalide, **401** token lipsa/invalid,
            **403** lipsesc drepturile (ex. rutele `/api/admin`), **404** ruta nu exista, **429** prea multe incercari
            (autentificare: 10 pe minut per IP, cu `Retry-After`).

            ### Timp real
            `GET /api/events` este un flux Server-Sent Events. Tipuri: `bid`, `closed`, `deleted`, `update`, `comment`,
            `notification`, `message`, `typing`, `read` (date: `{type, itemId, amount, username}`; la `notification`, `message`,
            `typing` si `read`, `username` e destinatarul). La 25 s vine un comentariu de mentinere.

            ### Conventii
            Sume in RON (`BigDecimal`), date in UTC fara fus orar (`2026-11-25T18:00:00`), id-uri intregi.
            """;

    // Display order of the groups in Swagger UI (descriptions come from @Tag on the controllers)
    static final List<String> TAG_ORDER = List.of("Autentificare", "Autentificare in doi pasi", "Obiecte", "Oferte",
            "Licitare automata", "Oferte private", "Comenzi", "Licitatii live", "Urmarite si recomandari", "Colectii", "Intrebari",
            "Chat", "Camera live", "Recenzii", "Raportari", "Utilizatori", "Portofel", "Plati", "Notificari", "Notificari push",
            "Alerte", "Asistent AI", "Statistici", "Clasament", "Categorii", "Imagini", "Evenimente live", "Sistem",
            "Erori", "Administrare");

    @Bean
    public OpenAPI auctionOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Licitatii.live API")
                        .version("1.0")
                        .description(DESCRIPTION)
                        .contact(new Contact().name("Licitatii.live").url("https://auction-engine.onrender.com/despre")))
                .servers(List.of(new Server().url("/").description("Serverul curent")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Tokenul de la POST /api/auth/login (fara cuvantul 'Bearer ')"))
                        .addSchemas(ERROR_SCHEMA, new ObjectSchema()
                                .description("Raspunsul oricarei erori")
                                .addProperty("eroare", new StringSchema().example("Oferta respinsa! Oferta minima este 105.00 RON"))));
    }

    // Public operations lose the padlock; every operation gets the error answers it can really return;
    // the groups are listed in TAG_ORDER
    @Bean
    public OpenApiCustomizer securityAndErrors() {
        return openApi -> {
            if (openApi.getTags() != null) {
                openApi.getTags().sort(java.util.Comparator.comparingInt((io.swagger.v3.oas.models.tags.Tag tag) -> {
                    int index = TAG_ORDER.indexOf(tag.getName());
                    return index < 0 ? TAG_ORDER.size() : index;
                }));
            }
            documentOperations(openApi);
        };
    }

    private static void documentOperations(OpenAPI openApi) {
        openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            boolean isPublic = SecurityConfig.isPublic(method.name(), path);
            if (isPublic) {
                operation.setSecurity(List.of());
            }
            ApiResponses responses = operation.getResponses() == null ? new ApiResponses() : operation.getResponses();
            responses.putIfAbsent("400", error("Regula incalcata sau date invalide"));
            if (!isPublic) {
                responses.putIfAbsent("401", error("Token lipsa, invalid sau expirat"));
            }
            if (path.startsWith("/api/admin")) {
                responses.putIfAbsent("403", error("Doar pentru administratori"));
            }
            if (method == PathItem.HttpMethod.POST && path.matches("/api/auth/(login|register|forgot-password|resend-verification|google|login/2fa)")) {
                responses.putIfAbsent("429", error("Prea multe incercari; reincearca dupa Retry-After secunde"));
            }
            operation.setResponses(responses);
        }));
    }

    private static ApiResponse error(String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType("application/json",
                new MediaType().schema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA))));
    }
}
