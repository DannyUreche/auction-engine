package com.DannyU.auction_engine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// The order created when an auction closes with a winner; the money waits in escrow (table "orders").
// Named PurchaseOrder because ORDER is a reserved word in JPQL.
@Data
@Entity
@Table(name = "orders")
public class PurchaseOrder {

    // PAID: buyer paid, money held; SHIPPED: seller sent it; DISPUTED: buyer complained, an admin decides;
    // COMPLETED: money released to the seller; REFUNDED: money back to the buyer
    public enum Status { PAID, SHIPPED, DISPUTED, COMPLETED, REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private Item item;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PAID;

    private String courier;
    private String awb;

    @Column(name = "dispute_reason", columnDefinition = "TEXT")
    private String disputeReason;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public boolean isOpen() {
        return status == Status.PAID || status == Status.SHIPPED || status == Status.DISPUTED;
    }
}
