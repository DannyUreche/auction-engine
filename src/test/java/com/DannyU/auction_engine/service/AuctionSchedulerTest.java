package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Fiecare test ruleaza intr-o tranzactie anulata la final, deci nu lasa date in baza
@SpringBootTest
@Transactional
class AuctionSchedulerTest {

    @Autowired
    private AuctionScheduler auctionScheduler;
    @Autowired
    private BidService bidService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void licitatiaExpirataSeInchideSiCelMaiMareOfertantCastiga() {
        User seller = createUser("vanzator");
        User cumparator1 = createUser("cumparator1");
        User cumparator2 = createUser("cumparator2");
        Item item = createItem(seller, LocalDateTime.now().plusHours(1));

        bidService.placeBid(item.getId().longValue(), cumparator1.getUsername(), new BigDecimal("150.00"));
        bidService.placeBid(item.getId().longValue(), cumparator2.getUsername(), new BigDecimal("200.00"));

        // Simulam trecerea timpului: licitatia a expirat
        item.setEndTime(LocalDateTime.now().minusMinutes(1));
        itemRepository.save(item);

        auctionScheduler.closeExpiredAuctions();

        assertThat(item.getActive()).isFalse();
        assertThat(item.getWinner()).isNotNull();
        assertThat(item.getWinner().getId()).isEqualTo(cumparator2.getId());
        assertThat(item.getCurrentPrice()).isEqualByComparingTo("200.00");
        assertThat(itemRepository.findByWinnerUsernameOrderByEndTimeDesc(cumparator2.getUsername()))
                .extracting(Item::getId)
                .containsExactly(item.getId());
    }

    @Test
    void licitatiaExpirataFaraOferteSeInchideFaraCastigator() {
        User seller = createUser("vanzator");
        Item item = createItem(seller, LocalDateTime.now().minusMinutes(1));

        auctionScheduler.closeExpiredAuctions();

        assertThat(item.getActive()).isFalse();
        assertThat(item.getWinner()).isNull();
    }

    @Test
    void licitatiaNeexpirataRamaneDeschisa() {
        User seller = createUser("vanzator");
        Item item = createItem(seller, LocalDateTime.now().plusHours(1));

        auctionScheduler.closeExpiredAuctions();

        assertThat(item.getActive()).isTrue();
        assertThat(item.getWinner()).isNull();
    }

    private User createUser(String prefix) {
        String unic = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(unic);
        user.setEmail(unic + "@test.ro");
        user.setPassword("parola-test");
        return userRepository.save(user);
    }

    private Item createItem(User owner, LocalDateTime endTime) {
        Item newItem = new Item();
        newItem.setName("Obiect test");
        newItem.setStartPrice(new BigDecimal("100.00"));
        newItem.setCurrentPrice(new BigDecimal("100.00"));
        newItem.setSeller(owner);
        newItem.setEndTime(endTime);
        return itemRepository.save(newItem);
    }
}
