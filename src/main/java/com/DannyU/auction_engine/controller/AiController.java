package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.AiListingService;
import com.DannyU.auction_engine.service.AiSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// AI assistants: listing suggestions for sellers (AiListingService) and plain-words search (AiSearchService).
// All routes need a logged-in user (each call costs money).
@io.swagger.v3.oas.annotations.tags.Tag(name = "Asistent AI", description = "Functii cu Claude: completarea anuntului din poza si cautarea in cuvinte obisnuite. Active doar cu ANTHROPIC_API_KEY.")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiListingService aiListingService;
    private final AiSearchService aiSearchService;

    public record SearchRequest(String query) {
    }

    public record AiConfig(boolean enabled) {
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Este asistentul AI activ?")
    @GetMapping("/config")
    public AiConfig config() {
        return new AiConfig(aiListingService.isEnabled());
    }

    // Multipart field "image": the photo to analyze; nothing is stored
    @io.swagger.v3.oas.annotations.Operation(summary = "Completeaza anuntul din poza (nume, descriere, categorie, pret)")
    @PostMapping(value = "/listing-suggestion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AiListingService.ListingSuggestion suggest(@RequestParam("image") MultipartFile image, Authentication authentication) {
        return aiListingService.suggest(image, authentication.getName());
    }

    // "Cauta cu AI": {query: "ceas vechi sub 500 lei"} -> the home page filters
    @io.swagger.v3.oas.annotations.Operation(summary = "Transforma o cautare scrisa liber in filtre")
    @PostMapping("/search")
    public AiSearchService.SearchFilters search(@RequestBody SearchRequest request, Authentication authentication) {
        return aiSearchService.interpret(request.query(), authentication.getName());
    }
}
