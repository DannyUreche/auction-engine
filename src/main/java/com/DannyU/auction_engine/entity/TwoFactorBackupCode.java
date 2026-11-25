package com.DannyU.auction_engine.entity;

import jakarta.persistence.*;
import lombok.Data;

// One-time backup code for 2FA (only its SHA-256 is stored)
@Data
@Entity
@Table(name = "two_factor_backup_codes")
public class TwoFactorBackupCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private boolean used = false;
}
