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

    @GetMapping("/config")
    public AiConfig config() {
        return new AiConfig(aiListingService.isEnabled());
    }

    // Multipart field "image": the photo to analyze; nothing is stored
    @PostMapping(value = "/listing-suggestion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AiListingService.ListingSuggestion suggest(@RequestParam("image") MultipartFile image, Authentication authentication) {
        return aiListingService.suggest(image, authentication.getName());
    }

    // "Cauta cu AI": {query: "ceas vechi sub 500 lei"} -> the home page filters
    @PostMapping("/search")
    public AiSearchService.SearchFilters search(@RequestBody SearchRequest request, Authentication authentication) {
        return aiSearchService.interpret(request.query(), authentication.getName());
    }
}
