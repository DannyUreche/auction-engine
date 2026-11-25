package com.DannyU.auction_engine.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

// A saved search: notify the user when a new auction matches it (any field left empty matches everything)
@Data
@Entity
@Table(name = "saved_alerts")
public class SavedAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String query;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Category category;

    @Column(name = "min_price")
    private BigDecimal minPrice;

    @Column(name = "max_price")
    private BigDecimal maxPrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean matches(Item item) {
        if (category != null && category != item.getCategory()) {
            return false;
        }
        BigDecimal price = item.getCurrentPrice();
        if (minPrice != null && price.compareTo(minPrice) < 0 || maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        String haystack = (item.getName() + " " + (item.getDescription() == null ? "" : item.getDescription())).toLowerCase(Locale.ROOT);
        return haystack.contains(needle);
    }
}
