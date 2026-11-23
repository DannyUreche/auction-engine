package com.DannyU.auction_engine.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_price", nullable = false)
    private BigDecimal startPrice;

    @Column(name = "current_price", nullable = false)
    private BigDecimal currentPrice;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // Timpul cand se termina licitatia
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Statusul licitatiei: true (deschisa), false (inchisa)
    @Column(nullable = false)
    private Boolean active = true;
}