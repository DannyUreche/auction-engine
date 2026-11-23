package com.DannyU.auction_engine.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    // Relatia catre utilizatorul care liciteaza (fara email in raspunsuri)
    @ManyToOne
    @JoinColumn(name = "bidder_id", nullable = false)
    @JsonIgnoreProperties("email")
    private User bidder;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}