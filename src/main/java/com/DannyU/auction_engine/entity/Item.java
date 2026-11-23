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
        return buyNowPrice != null && Boolean.TRUE.equals(active) && currentPrice.compareTo(buyNowPrice) < 0;
    }
}