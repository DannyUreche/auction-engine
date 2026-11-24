package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.dto.AdminDtos;
import com.DannyU.auction_engine.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

// Cifrele afisate pe pagina principala (publice, fara date personale)
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final AdminService adminService;

    public record PublicStats(long users, long activeItems, long bids, long soldItems, BigDecimal soldValue) {
    }

    @GetMapping("/api/stats")
    public PublicStats stats() {
        AdminDtos.Stats stats = adminService.stats();
        return new PublicStats(stats.users(), stats.activeItems(), stats.bids(), stats.soldItems(), stats.soldValue());
    }
}
