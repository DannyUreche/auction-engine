package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.Question;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.entity.WatchedItem;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.QuestionRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.repository.WatchedItemRepository;
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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Monthly leaderboard and challenges
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LeaderboardApiTest {

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
    @Autowired
    private WatchedItemRepository watchedItemRepository;
    @Autowired
    private QuestionRepository questionRepository;

    @Test
    void challengesFollowThisMonthsActivity() throws Exception {
        User seller = user("vanzator");
        User player = user("jucator");
        String token = "Bearer " + jwtService.generateToken(player.getUsername());

        mockMvc.perform(get("/api/leaderboard/me").header("Authorization", token))
                .andExpect(jsonPath("$.completed").value(0))
                .andExpect(jsonPath("$.challenges.length()").value(5));

        Category[] categories = {Category.MODA, Category.SPORT, Category.VEHICULE};
        for (int i = 0; i < 10; i++) {
            Item item = item(seller, categories[i % 3]);
            Bid bid = new Bid();
            bid.setItem(item);
            bid.setBidder(player);
            bid.setAmount(new BigDecimal("150"));
            bidRepository.save(bid);
            if (i < 5) {
                WatchedItem watch = new WatchedItem();
                watch.setUser(player);
                watch.setItem(item);
                watchedItemRepository.save(watch);
            }
            if (i == 0) {
                item.setActive(false);
                item.setWinner(player);
                // A big sale, so the seller is in the top 10 whatever else the database holds
                item.setCurrentPrice(new BigDecimal("9999999"));
                itemRepository.save(item);
                Question question = new Question();
                question.setItem(item);
                question.setAuthor(player);
                question.setText("Mai e valabil?");
                questionRepository.save(question);
            }
        }

        mockMvc.perform(get("/api/leaderboard/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(5))
                .andExpect(jsonPath("$.challenges[0].progress").value(10))
                .andExpect(jsonPath("$.bidderRank").isNumber());
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellers[*].username", hasItem(seller.getUsername())));
        mockMvc.perform(get("/api/users/" + player.getUsername() + "/badges"))
                .andExpect(jsonPath("$[*].code", hasItem("CHALLENGER")));
        mockMvc.perform(get("/api/leaderboard/me")).andExpect(status().isUnauthorized());
    }

    private User user(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }

    private Item item(User seller, Category category) {
        Item item = new Item();
        item.setName("Obiect clasament");
        item.setStartPrice(new BigDecimal("100"));
        item.setCurrentPrice(new BigDecimal("150"));
        item.setSeller(seller);
        item.setCategory(category);
        item.setEndTime(LocalDateTime.now().plusHours(2));
        return itemRepository.save(item);
    }
}
