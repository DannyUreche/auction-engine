package com.DannyU.auction_engine.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// Un mesaj din chat-ul dintre castigator si vanzator
@Data
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // When the recipient opened the chat after this message arrived (null = unread)
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // In JSON trimitem doar numele expeditorului
    public String getSenderUsername() {
        return sender == null ? null : sender.getUsername();
    }
}
