package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Lista de favorite ("Urmaresc") a utilizatorului logat
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    public record WatchStatus(Integer itemId, boolean watching) {
    }

    // Obiectele urmarite de utilizatorul logat
    @GetMapping("/watched")
    public List<Item> getWatchedItems(Authentication authentication) {
        return watchlistService.watchedItems(authentication.getName());
    }

    @PostMapping("/{id}/watch")
    public WatchStatus watch(@PathVariable Integer id, Authentication authentication) {
        watchlistService.watch(id, authentication.getName());
        return new WatchStatus(id, true);
    }

    @DeleteMapping("/{id}/watch")
    public WatchStatus unwatch(@PathVariable Integer id, Authentication authentication) {
        watchlistService.unwatch(id, authentication.getName());
        return new WatchStatus(id, false);
    }
}
