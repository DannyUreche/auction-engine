package com.DannyU.auction_engine.entity;

import com.DannyU.auction_engine.service.BidRules;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // Pozele obiectului (tabelul item_photos), in ordinea afisarii; prima este poza principala.
    // In raspunsuri apar ca imageUrls / imageUrl (vezi mai jos). SUBSELECT: la o lista de obiecte,
    // pozele tuturor se incarca dintr-o singura interogare (nu cate una pentru fiecare obiect)
    @JsonIgnore
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_photos", joinColumns = @JoinColumn(name = "item_id"))
    @OrderColumn(name = "position")
    @Column(name = "image_filename", nullable = false)
    @Fetch(FetchMode.SUBSELECT)
    private List<String> imageFilenames = new ArrayList<>();

    // Categoria obiectului (pentru filtrele de pe pagina principala)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category = Category.ALTELE;

    // Pret optional "Cumpara acum" (null = obiectul se vinde doar prin licitatie)
    @Column(name = "buy_now_price")
    private BigDecimal buyNowPrice;

    // Scheduled auction: bidding opens at startTime (null = open since creation)
    @Column(name = "start_time")
    private LocalDateTime startTime;

    // Whether the "auction started" notifications were sent (only relevant for scheduled auctions)
    @JsonIgnore
    @Column(name = "start_notified", nullable = false)
    private boolean startNotified = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "auction_type", nullable = false, length = 10)
    private AuctionType auctionType = AuctionType.ENGLISH;

    // Dutch auction only: lowest price and how much the price drops every minute
    @Column(name = "floor_price")
    private BigDecimal floorPrice;

    @Column(name = "price_drop")
    private BigDecimal priceDrop;

    // County where the item is (one of Counties.ALL, optional; filter on the home page)
    @Column(length = 60)
    private String county;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Delivery delivery = Delivery.BOTH;

    // Part of a live show (LiveShowService): the show and the item's place in it
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "live_show_id")
    private LiveShow liveShow;

    @Column(name = "show_position")
    private Integer showPosition;

    public Integer getLiveShowId() {
        return liveShow == null ? null : liveShow.getId();
    }

    // Has the AI moderation looked at this listing yet? (AiModerationService; new listings start as false)
    @JsonIgnore
    @Column(name = "ai_checked", nullable = false)
    private boolean aiChecked = true;

    // Unique visits of the item page (LiveRoomService); shown only in the seller's statistics
    @JsonIgnore
    // Written only by the UPDATE in LiveRoomService, never by saving the entity (a save would overwrite it with a stale value)
    @Column(name = "view_count", nullable = false, insertable = false, updatable = false)
    private int viewCount = 0;

    // Has the auction opened yet? (exposed as "started" in JSON)
    public boolean isStarted() {
        return startTime == null || !LocalDateTime.now().isBefore(startTime);
    }

    // Dutch auction price at a given moment: start price minus one drop per full minute, never below the floor
    public BigDecimal dutchPriceAt(LocalDateTime moment) {
        long minutes = Math.max(0, java.time.Duration.between(startTime, moment).toMinutes());
        return startPrice.subtract(priceDrop.multiply(BigDecimal.valueOf(minutes))).max(floorPrice);
    }

    // Cate poze poate avea un obiect
    public static final int MAX_IMAGES = 5;

    // Adresele la care browserul poate incarca pozele, in ordine
    public List<String> getImageUrls() {
        return imageFilenames.stream().map(filename -> "/api/images/" + filename).toList();
    }

    // Poza principala (cea de pe card); null daca obiectul nu are poze
    public String getImageUrl() {
        return imageFilenames.isEmpty() ? null : "/api/images/" + imageFilenames.getFirst();
    }

    // Cea mai mica oferta acceptata acum (pretul curent + pasul minim) - afisata in interfata
    public BigDecimal getMinimumBid() {
        return currentPrice == null ? null : BidRules.minimumNextBid(currentPrice);
    }

    // "Cumpara acum" e disponibil cat timp licitatia e deschisa si ofertele n-au ajuns la acel pret
    public boolean isBuyNowAvailable() {
        if (!Boolean.TRUE.equals(active) || !isStarted()) {
            return false;
        }
        // A Dutch auction can always be bought (at its current price) while it runs
        if (auctionType == AuctionType.DUTCH) {
            return true;
        }
        return buyNowPrice != null && currentPrice.compareTo(buyNowPrice) < 0;
    }
}