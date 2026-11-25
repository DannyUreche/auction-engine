package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.repository.DatabaseProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

// Lightweight public health check: used by the keep-alive workflow (so the free Render instance
// doesn't spin down) and handy for uptime monitors. It also touches the database, so the response
// says whether the whole app works, not just the web server.
@io.swagger.v3.oas.annotations.tags.Tag(name = "Sistem", description = "Starea aplicatiei si a bazei de date.")
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DatabaseProbe probe;

    public record Health(String status, boolean database, Instant time) {
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Verificare de functionare (inclusiv baza de date)")
    @GetMapping("/api/health")
    public Health health() {
        boolean database;
        try {
            probe.ping();
            database = true;
        } catch (RuntimeException e) {
            database = false;
        }
        return new Health(database ? "ok" : "degraded", database, Instant.now());
    }
}
