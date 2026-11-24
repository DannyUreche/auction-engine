package com.DannyU.auction_engine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// A browser/device of a user that accepted push notifications (the data comes from PushManager.subscribe)
@Data
@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String endpoint;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String p256dh;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String auth;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
