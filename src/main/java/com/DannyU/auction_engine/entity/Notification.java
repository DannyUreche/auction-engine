package com.DannyU.auction_engine.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// O notificare pentru un utilizator (afisata la clopotelul din bara de sus)
@Data
@Entity
@Table(name = "notifications")
public class Notification {

    // OUTBID = oferta ta a fost depasita, NEW_BID = oferta noua la obiectul tau,
    // WON = ai castigat, SOLD = obiectul tau s-a vandut, UNSOLD = licitatia s-a incheiat fara oferte,
    // MESSAGE = mesaj nou in chat-ul unui obiect vandut
    // STARTED = a scheduled auction you watch (or sell) has started; QUESTION / ANSWER = questions about items
    public enum Type { OUTBID, NEW_BID, WON, SOLD, UNSOLD, MESSAGE, STARTED, QUESTION, ANSWER, ALERT, OFFER, OFFER_REPLY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // Doar id-ul obiectului (pentru link), nu obiectul intreg
    @Column(name = "item_id")
    private Integer itemId;

    @Column(nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
