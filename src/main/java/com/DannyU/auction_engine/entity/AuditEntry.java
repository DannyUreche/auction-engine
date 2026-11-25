package com.DannyU.auction_engine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// One admin action in the audit log (AuditService). The target label is copied, so it survives deletions.
@Data
@Entity
@Table(name = "admin_audit_log")
public class AuditEntry {

    public enum Action {
        CLOSE_AUCTION, DELETE_ITEM, FEATURE_ITEM, CLEAR_FEATURED, DISMISS_REPORT, BLOCK_USER, UNBLOCK_USER,
        DEMO_GENERATE, DEMO_DELETE, RESOLVE_ERROR, DELETE_RESOLVED_ERRORS, TEST_EMAIL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Action action;

    @Column(name = "target_type", length = 20)
    private String targetType;

    @Column(name = "target_id")
    private Integer targetId;

    @Column(name = "target_label", length = 200)
    private String targetLabel;

    @Column(length = 300)
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
