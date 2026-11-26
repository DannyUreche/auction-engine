package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Verifiable auctions: the database trigger chains the bids, Java recomputes the same hashes,
// and any later change to the stored history breaks the chain
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BidChainTest {

    @Autowired
    private BidService bidService;
    @Autowired
    private BidChainService bidChainService;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void bidsFormAChainThatDetectsTampering() throws Exception {
        User seller = user("lant_v");
        User a = user("lant_a");
        User b = user("lant_b");
        Item item = item(seller);

        Bid first = bidService.placeBid(item.getId(), a.getUsername(), new BigDecimal("105"));
        Bid second = bidService.placeBid(item.getId(), b.getUsername(), new BigDecimal("110"));
        Bid third = bidService.placeBid(item.getId(), a.getUsername(), new BigDecimal("120.50"));

        // The trigger filled the hashes and they were read back into the entities
        assertThat(first.getPrevHash()).isEqualTo(BidChainService.GENESIS);
        assertThat(second.getPrevHash()).isEqualTo(first.getHash());
        assertThat(third.getPrevHash()).isEqualTo(second.getHash());
        // SQL and Java compute the same hash from the same canonical text
        assertThat(BidChainService.hash(third)).isEqualTo(third.getHash());

        BidChainService.Verification ok = bidChainService.verify(item.getId());
        assertThat(ok.valid()).isTrue();
        assertThat(ok.length()).isEqualTo(3);
        assertThat(ok.head()).isEqualTo(third.getHash());
        mockMvc.perform(get("/api/bids/" + item.getId() + "/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.head").value(third.getHash()));

        // Someone changes an amount directly in the database
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE bids SET amount = 109 WHERE id = :id").setParameter("id", second.getId()).executeUpdate();
        entityManager.clear();
        BidChainService.Verification changed = bidChainService.verify(item.getId());
        assertThat(changed.valid()).isFalse();
        assertThat(changed.brokenAt()).isEqualTo(second.getId());

        // ...or removes a bid: the next one no longer links to its predecessor
        entityManager.createNativeQuery("UPDATE bids SET amount = 110 WHERE id = :id").setParameter("id", second.getId()).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM bids WHERE id = :id").setParameter("id", second.getId()).executeUpdate();
        entityManager.clear();
        BidChainService.Verification removed = bidChainService.verify(item.getId());
        assertThat(removed.valid()).isFalse();
        assertThat(removed.brokenAt()).isEqualTo(third.getId());
    }

    @Test
    void chainsAreSeparatePerItem() {
        User seller = user("lant_v");
        User a = user("lant_a");
        Item one = item(seller);
        Item two = item(seller);
        bidService.placeBid(one.getId(), a.getUsername(), new BigDecimal("105"));
        Bid firstOnTwo = bidService.placeBid(two.getId(), a.getUsername(), new BigDecimal("105"));
        assertThat(firstOnTwo.getPrevHash()).isEqualTo(BidChainService.GENESIS);
        List<Bid> chain = bidRepository.findByItemIdOrderByIdAsc(two.getId());
        assertThat(chain).hasSize(1);
    }

    private User user(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }

    private Item item(User seller) {
        Item item = new Item();
        item.setName("Obiect cu lant");
        item.setStartPrice(new BigDecimal("100"));
        item.setCurrentPrice(new BigDecimal("100"));
        item.setSeller(seller);
        item.setEndTime(LocalDateTime.now().plusHours(2));
        return itemRepository.save(item);
    }
}
