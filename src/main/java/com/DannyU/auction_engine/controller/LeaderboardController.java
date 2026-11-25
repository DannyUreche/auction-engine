package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Monthly leaderboard (public) and the logged-in user's monthly challenges
@RestController
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/api/leaderboard")
    public LeaderboardService.Leaderboard leaderboard() {
        return leaderboardService.leaderboard();
    }

    @GetMapping("/api/leaderboard/me")
    public LeaderboardService.MyMonth myMonth(Authentication authentication) {
        return leaderboardService.myMonth(authentication.getName());
    }
}
