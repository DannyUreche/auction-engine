package com.DannyU.auction_engine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// Un obiect urmarit (favorit) de un utilizator
@Data
@Entity
@Table(name = "watched_items")
public class WatchedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
