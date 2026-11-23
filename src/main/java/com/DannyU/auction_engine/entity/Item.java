package com.DannyU.auction_engine.entity;

import com.DannyU.auction_engine.service.BidRules;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    // Email-ul vanzatorului nu se trimite in raspunsuri (e vizibil doar in /api/users/me)
    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnoreProperties("email")
    private User seller;

    // Timpul cand se termina licitatia
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Statusul licitatiei: true (deschisa), false (inchisa)
    @Column(nullable = false)
    private Boolean active = true;

    // Castigatorul licitatiei: se seteaza la inchidere, ramane null daca nu exista oferte
    @ManyToOne
    @JoinColumn(name = "winner_id")
    @JsonIgnoreProperties("email")
    private User winner;

    // Numele fisierului imaginii; in raspunsuri apare ca imageUrl (vezi getImageUrl)
    @JsonIgnore
    @Column(name = "image_filename")
    private String imageFilename;

    // Pret optional "Cumpara acum" (null = obiectul se vinde doar prin licitatie)
    @Column(name = "buy_now_price")
    private BigDecimal buyNowPrice;

    // Adresa la care browserul poate incarca imaginea (null daca obiectul nu are imagine)
    public String getImageUrl() {
        return imageFilename == null ? null : "/api/images/" + imageFilename;
    }

    // Cea mai mica oferta acceptata acum (pretul curent + pasul minim) - afisata in interfata
    public BigDecimal getMinimumBid() {
        return currentPrice == null ? null : BidRules.minimumNextBid(currentPrice);
    }

    // "Cumpara acum" e disponibil cat timp licitatia e deschisa si ofertele n-au ajuns la acel pret
    public boolean isBuyNowAvailable() {
        return buyNowPrice != null && Boolean.TRUE.equals(active) && currentPrice.compareTo(buyNowPrice) < 0;
    }
}