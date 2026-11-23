package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Fiecare test ruleaza intr-o tranzactie anulata la final, deci nu lasa date in baza
@SpringBootTest
@Transactional
@RecordApplicationEvents
class BidServiceTest {

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private BidService bidService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BidRepository bidRepository;

    private User seller;
    private User bidder;
    private Item item;

    @BeforeEach
    void setUp() {
        seller = createUser("vanzator");
        bidder = createUser("cumparator");
        item = createItem(seller, new BigDecimal("100.00"), LocalDateTime.now().plusHours(1));
    }

    @Test
    void ofertaValidaEsteSalvataSiActualizeazaPretul() {
        Bid bid = bidService.placeBid(itemId(), bidder.getUsername(), new BigDecimal("150.00"));

        assertThat(bid.getId()).isNotNull();
        assertThat(bid.getBidder().getId()).isEqualTo(bidder.getId());
        assertThat(bid.getAmount()).isEqualByComparingTo("150.00");
        assertThat(item.getCurrentPrice()).isEqualByComparingTo("150.00");
        assertThat(bidRepository.findByItemIdOrderByAmountDesc(itemId())).hasSize(1);
    }

    @Test
    void ofertaValidaPublicaUnEvenimentPentruBrowsere() {
        bidService.placeBid(itemId(), bidder.getUsername(), new BigDecimal("150.00"));

        assertThat(events.stream(AuctionEvent.class))
                .containsExactly(AuctionEvent.bid(item.getId(), new BigDecimal("150.00"), bidder.getUsername()));
    }

    @Test
    void ofertaRespinsaNuPublicaEveniment() {
        assertThatThrownBy(() -> bidService.placeBid(itemId(), seller.getUsername(), new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(events.stream(AuctionEvent.class)).isEmpty();
    }

    @Test
    void ofertaEgalaCuPretulCurentEsteRespinsa() {
        assertThatThrownBy(() -> bidService.placeBid(itemId(), bidder.getUsername(), new BigDecimal("100.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Suma trebuie sa fie mai mare");
    }

    @Test
    void ofertaMaiMicaDecatOfertaAnterioaraEsteRespinsa() {
        User altCumparator = createUser("alt");
        bidService.placeBid(itemId(), bidder.getUsername(), new BigDecimal("200.00"));

        assertThatThrownBy(() -> bidService.placeBid(itemId(), altCumparator.getUsername(), new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Suma trebuie sa fie mai mare");
        assertThat(item.getCurrentPrice()).isEqualByComparingTo("200.00");
    }

    @Test
    void vanzatorulNuPoateLicitaLaPropriulObiect() {
        assertThatThrownBy(() -> bidService.placeBid(itemId(), seller.getUsername(), new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nu poti licita pentru propriul obiect!");
    }

    @Test
    void nuSePoateLicitaDupaExpirareaTimpului() {
        Item expirat = createItem(seller, new BigDecimal("100.00"), LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> bidService.placeBid(expirat.getId(),bidder.getUsername(), new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Licitatia pentru acest obiect s-a incheiat deja!");
    }

    @Test
    void nuSePoateLicitaLaOLicitatieInchisa() {
        item.setActive(false);
        itemRepository.save(item);

        assertThatThrownBy(() -> bidService.placeBid(itemId(), bidder.getUsername(), new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Aceasta licitatie a fost inchisa oficial!");
    }

    @Test
    void obiectInexistentEsteRespins() {
        assertThatThrownBy(() -> bidService.placeBid(-1,bidder.getUsername(), new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Obiectul nu a fost gasit!");
    }

    @Test
    void utilizatorInexistentEsteRespins() {
        assertThatThrownBy(() -> bidService.placeBid(itemId(), "nu_exista_" + UUID.randomUUID(), new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Utilizatorul nu a fost gasit!");
    }

    @Test
    void ofertelePentruUnObiectSuntOrdonateDescrescator() {
        User altCumparator = createUser("alt");
        bidService.placeBid(itemId(), bidder.getUsername(), new BigDecimal("120.00"));
        bidService.placeBid(itemId(), altCumparator.getUsername(), new BigDecimal("180.00"));

        assertThat(bidService.getBidsForItem(itemId()))
                .extracting(Bid::getAmount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("180.00"), new BigDecimal("120.00"));
    }

    private Integer itemId() {
        return item.getId();
    }

    private User createUser(String prefix) {
        String unic = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(unic);
        user.setEmail(unic + "@test.ro");
        user.setPassword("parola-test");
        return userRepository.save(user);
    }

    private Item createItem(User owner, BigDecimal startPrice, LocalDateTime endTime) {
        Item newItem = new Item();
        newItem.setName("Obiect test");
        newItem.setStartPrice(startPrice);
        newItem.setCurrentPrice(startPrice);
        newItem.setSeller(owner);
        newItem.setEndTime(endTime);
        return itemRepository.save(newItem);
    }
}
