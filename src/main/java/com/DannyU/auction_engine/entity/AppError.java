package com.DannyU.auction_engine.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// One kind of error seen by the error monitor (see ErrorMonitorService)
@Data
@Entity
@Table(name = "app_errors")
public class AppError {

    public enum Source { SERVER, CLIENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Source source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 300)
    private String path;

    @Column(length = 50)
    private String username;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(nullable = false)
    private int occurrences = 1;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen = LocalDateTime.now();

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen = LocalDateTime.now();

    @Column(nullable = false)
    private boolean resolved = false;
}
