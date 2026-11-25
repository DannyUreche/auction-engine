package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Monthly leaderboard (public) and the logged-in user's monthly challenges
@io.swagger.v3.oas.annotations.tags.Tag(name = "Clasament", description = "Clasamentul lunii si provocarile lunare.")
@RestController
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Clasamentul lunii")
    @GetMapping("/api/leaderboard")
    public LeaderboardService.Leaderboard leaderboard() {
        return leaderboardService.leaderboard();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Provocarile mele din aceasta luna")
    @GetMapping("/api/leaderboard/me")
    public LeaderboardService.MyMonth myMonth(Authentication authentication) {
        return leaderboardService.myMonth(authentication.getName());
    }
}
