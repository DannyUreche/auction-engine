package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.AiListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// AI assistant for sellers (see AiListingService). Both routes need a logged-in user.
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiListingService aiListingService;

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
}
