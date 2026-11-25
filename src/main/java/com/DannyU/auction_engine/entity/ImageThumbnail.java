package com.DannyU.auction_engine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// Small JPEG version of an item photo (same filename as the original in item_images)
@Data
@Entity
@Table(name = "image_thumbnails")
public class ImageThumbnail {

    @Id
    private String filename;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
