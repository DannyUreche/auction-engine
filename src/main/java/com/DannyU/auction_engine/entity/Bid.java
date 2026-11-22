package com.DannyU.auction_engine.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relatia catre obiectul pe care se liciteaza
    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Relatia catre utilizatorul care liciteaza
    @ManyToOne
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}