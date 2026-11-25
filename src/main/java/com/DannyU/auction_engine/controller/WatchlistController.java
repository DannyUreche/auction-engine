package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Lista de favorite ("Urmaresc") a utilizatorului logat
@io.swagger.v3.oas.annotations.tags.Tag(name = "Urmarite si recomandari", description = "Obiectele urmarite si recomandarile personale.")
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final com.DannyU.auction_engine.service.RecommendationService recommendationService;

    public record WatchStatus(Integer itemId, boolean watching) {
    }

    // Obiectele urmarite de utilizatorul logat
    @io.swagger.v3.oas.annotations.Operation(summary = "Obiectele pe care le urmaresc")
    @GetMapping("/watched")
    public List<Item> getWatchedItems(Authentication authentication) {
        return watchlistService.watchedItems(authentication.getName());
    }

    // Recommendations for the logged-in user (home page)
    @io.swagger.v3.oas.annotations.Operation(summary = "Recomandari pentru mine")
    @GetMapping("/recommended")
    public List<Item> recommended(Authentication authentication) {
        return recommendationService.recommendedFor(authentication.getName());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Urmareste un obiect")
    @PostMapping("/{id}/watch")
    public WatchStatus watch(@PathVariable Integer id, Authentication authentication) {
        watchlistService.watch(id, authentication.getName());
        return new WatchStatus(id, true);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Nu mai urmari obiectul")
    @DeleteMapping("/{id}/watch")
    public WatchStatus unwatch(@PathVariable Integer id, Authentication authentication) {
        watchlistService.unwatch(id, authentication.getName());
        return new WatchStatus(id, false);
    }
}
