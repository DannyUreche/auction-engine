package com.DannyU.auction_engine.dto;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

// Raspunsurile rutelor /api/admin/**
public final class AdminDtos {

    private AdminDtos() {
    }

    public record Stats(long users, long items, long activeItems, long bids, long soldItems, BigDecimal soldValue) {
    }

    public record UserRow(Integer id, String username, String email, LocalDateTime createdAt,
                          long itemsCount, long bidsCount, boolean admin) {
    }

    public record ItemRow(Integer id, String name, String imageUrl, String seller, BigDecimal startPrice,
                          BigDecimal currentPrice, boolean active, LocalDateTime endTime,
                          long bidsCount, String winner) {
    }

    // Datele pentru graficele din pagina de administrare
    public record Charts(List<DayCount> bidsPerDay, List<CategorySales> salesByCategory, List<SellerSales> topSellers) {
    }

    public record DayCount(java.time.LocalDate day, long bids) {
    }

    public record CategorySales(String category, String label, String icon, long sold, BigDecimal value) {
    }

    public record SellerSales(String username, long sold, BigDecimal value) {
    }
}
