package com.DannyU.auction_engine.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// A private price offer for an item (see OfferService)
@Data
@Entity
@Table(name = "offers")
public class Offer {

    public enum Status { PENDING, COUNTERED, ACCEPTED, REJECTED, WITHDRAWN, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 300)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.PENDING;

    @Column(name = "counter_amount")
    private BigDecimal counterAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public String getBuyerUsername() {
        return buyer.getUsername();
    }

    public Integer getItemId() {
        return item.getId();
    }

    public boolean isOpen() {
        return status == Status.PENDING || status == Status.COUNTERED;
    }
}
