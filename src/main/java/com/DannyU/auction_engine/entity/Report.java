package com.DannyU.auction_engine.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// A user's report about a suspicious item, handled by an admin
@Data
@Entity
@Table(name = "reports")
public class Report {

    public enum Reason { FRAUD, PROHIBITED, WRONG_CATEGORY, OFFENSIVE, OTHER }

    public enum Status { OPEN, DISMISSED }

    // USER = reported by a person, AI = flagged by the AI moderation (no reporter)
    public enum Source { USER, AI }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Reason reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Source source = Source.USER;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
