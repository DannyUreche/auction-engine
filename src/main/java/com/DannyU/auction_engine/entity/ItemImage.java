package com.DannyU.auction_engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

// Imaginea unui obiect, salvata direct in baza de date
@Data
@Entity
@Table(name = "item_images")
public class ItemImage {

    // Nume generat de noi: UUID + extensie (ex: 3f2a...c1.jpg)
    @Id
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
