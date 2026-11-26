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

    // true = oferta plasata de licitarea automata (nu de utilizator, direct)
    @Column(nullable = false)
    private boolean automatic = false;

    // Microseconds, the precision PostgreSQL stores: the hash is computed from exactly this value
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);

    // Verifiable history (V39): SHA-256 chain per item, written by the database trigger bids_chain and read back
    // after the insert. hash = sha256("id|prevHash|itemId|bidderId|amount|createdAt|automatic").
    @org.hibernate.annotations.Generated(event = org.hibernate.generator.EventType.INSERT)
    @Column(name = "prev_hash", insertable = false, updatable = false)
    private String prevHash;

    @org.hibernate.annotations.Generated(event = org.hibernate.generator.EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private String hash;
}