package com.DannyU.auction_engine.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data // Generhează automat Getteri, Setteri, toString etc. (datorită Lombok)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Virtual wallet balance (test credits). Never part of the public JSON - see /api/wallet
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false)
    private java.math.BigDecimal balance = new java.math.BigDecimal("10000.00");

    // Blocked by an admin: cannot log in, tokens stop working
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false)
    private boolean blocked = false;

    // False until the email link is opened (only when sending emails is configured)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = true;

}