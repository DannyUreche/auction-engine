package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Reputation badges computed from activity
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BadgeApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BidRepository bidRepository;

    @Test
    void badgesFollowActivity() throws Exception {
        User seller = createUser("vanzator", LocalDateTime.now());
        User buyer = createUser("colectionar", LocalDateTime.now().minusYears(2));

        mockMvc.perform(get("/api/users/" + seller.getUsername() + "/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // The buyer opens the bidding on 5 items and wins 3 of them
        for (int i = 0; i < 5; i++) {
            Item item = createItem(seller);
            Bid bid = new Bid();
            bid.setItem(item);
            bid.setBidder(buyer);
            bid.setAmount(new BigDecimal("150"));
            bid.setCreatedAt(LocalDateTime.now().minusHours(1));
            bidRepository.save(bid);
            if (i < 3) {
                item.setActive(false);
                item.setWinner(buyer);
                itemRepository.save(item);
            }
        }

        mockMvc.perform(get("/api/users/" + buyer.getUsername() + "/badges"))
                .andExpect(jsonPath("$[*].code", hasItems("COLLECTOR", "FIRST_BIDDER", "VETERAN")));
        mockMvc.perform(get("/api/users/" + buyer.getUsername() + "/profile"))
                .andExpect(jsonPath("$.badges", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.badges[0].label").isNotEmpty());
    }

    private User createUser(String prefix, LocalDateTime createdAt) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        user.setCreatedAt(createdAt);
        return userRepository.save(user);
    }

    private Item createItem(User owner) {
        Item item = new Item();
        item.setName("Obiect");
        item.setStartPrice(new BigDecimal("100"));
        item.setCurrentPrice(new BigDecimal("150"));
        item.setSeller(owner);
        item.setCategory(Category.ALTELE);
        item.setEndTime(LocalDateTime.now().plusHours(1));
        return itemRepository.save(item);
    }
}
