package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// "Licitatia saptamanii": chosen by an admin, with the top bidders
@SpringBootTest(properties = "app.admin-usernames=" + FeaturedApiTest.ADMIN)
@AutoConfigureMockMvc
@Transactional
class FeaturedApiTest {

    static final String ADMIN = "admin_test_featured";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BidRepository bidRepository;

    @Test
    void adminChoosesTheFeaturedAuction() throws Exception {
        String admin = "Bearer " + jwtService.generateToken(user(ADMIN, false).getUsername());
        User seller = user("vanzator", true);
        User ana = user("ana", true);
        User dan = user("dan", true);
        Item item = item(seller, true);
        bid(item, ana, "120");
        bid(item, dan, "130");
        bid(item, ana, "140");

        mockMvc.perform(post("/api/admin/items/" + item.getId() + "/feature").header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/items/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.id").value(item.getId()))
                .andExpect(jsonPath("$.chosenByAdmin").value(true))
                .andExpect(jsonPath("$.bids").value(3))
                .andExpect(jsonPath("$.leaderboard[0].username").value(ana.getUsername()))
                .andExpect(jsonPath("$.leaderboard[0].amount").value(140))
                .andExpect(jsonPath("$.leaderboard[0].bids").value(2))
                .andExpect(jsonPath("$.leaderboard[1].username").value(dan.getUsername()));

        Item closed = item(seller, false);
        mockMvc.perform(post("/api/admin/items/" + closed.getId() + "/feature").header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/items/" + item.getId() + "/feature")
                        .header("Authorization", "Bearer " + jwtService.generateToken(seller.getUsername())))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/featured").header("Authorization", admin)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/items/featured")).andExpect(jsonPath("$.chosenByAdmin").value(false));
    }

    private void bid(Item item, User bidder, String amount) {
        Bid bid = new Bid();
        bid.setItem(item);
        bid.setBidder(bidder);
        bid.setAmount(new BigDecimal(amount));
        bidRepository.save(bid);
        item.setCurrentPrice(new BigDecimal(amount));
        itemRepository.save(item);
    }

    private User user(String name, boolean unique) {
        String username = unique ? name + "_" + UUID.randomUUID().toString().substring(0, 8) : name;
        User user = userRepository.findByUsername(username).orElseGet(User::new);
        user.setUsername(username);
        user.setEmail(username + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }

    private Item item(User seller, boolean active) {
        Item item = new Item();
        item.setName("Obiect saptamana");
        item.setStartPrice(new BigDecimal("100"));
        item.setCurrentPrice(new BigDecimal("100"));
        item.setSeller(seller);
        item.setCategory(Category.ALTELE);
        item.setActive(active);
        item.setEndTime(LocalDateTime.now().plusDays(2));
        return itemRepository.save(item);
    }
}
