package com.DannyU.auction_engine.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Cheapest possible database round trip, for the health check
@Component
@RequiredArgsConstructor
public class DatabaseProbe {

    private final JdbcTemplate jdbcTemplate;

    public void ping() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    }
}
