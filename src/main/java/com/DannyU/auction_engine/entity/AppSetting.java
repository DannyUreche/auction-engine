package com.DannyU.auction_engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

// A server setting stored in the database (e.g. the Web Push key pair)
@Data
@Entity
@Table(name = "app_settings")
public class AppSetting {

    @Id
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;
}
