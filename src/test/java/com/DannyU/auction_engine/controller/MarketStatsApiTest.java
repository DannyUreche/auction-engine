package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.MarketStatsService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Public market statistics ("Piata in cifre"): compared before/after adding one heavily contested sale
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MarketStatsApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MarketStatsService marketStatsService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BidRepository bidRepository;

    @Test
    void saleShowsUpInEverySection() throws Exception {
        String before = market();
        int soldBefore = JsonPath.read(before, "$.totals.soldLast30");
        int bidsBefore = JsonPath.read(before, "$.totals.bidsLast30");
        long heatBefore = heatSum(before);

        User seller = user("piata_v");
        User a = user("piata_a");
        User b = user("piata_b");
        Item item = new Item();
        item.setName("Obiect foarte disputat");
        item.setStartPrice(new BigDecimal("100"));
        item.setCurrentPrice(new BigDecimal("400"));
        item.setSeller(seller);
        item.setCategory(Category.SPORT);
        item.setEndTime(LocalDateTime.now().minusMinutes(5));
        item.setActive(false);
        item.setWinner(b);
        item = itemRepository.save(item);
        // Enough bids to be the most contested auction of the month, whatever else the database holds
        List<Bid> bids = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            Bid bid = new Bid();
            bid.setItem(item);
            bid.setBidder(i % 2 == 0 ? a : b);
            bid.setAmount(new BigDecimal(101 + i));
            bids.add(bid);
        }
        bidRepository.saveAllAndFlush(bids);

        String after = market();
        assertThat((int) JsonPath.read(after, "$.totals.soldLast30")).isEqualTo(soldBefore + 1);
        assertThat((int) JsonPath.read(after, "$.totals.bidsLast30")).isEqualTo(bidsBefore + 300);
        assertThat(heatSum(after)).isEqualTo(heatBefore + 300);
        assertThat((int) JsonPath.read(after, "$.contested[0].id")).isEqualTo(item.getId());
        assertThat((int) JsonPath.read(after, "$.contested[0].bidders")).isEqualTo(2);
        assertThat((List<Object>) JsonPath.read(after, "$.categories")).hasSize(Category.values().length);
        assertThat((List<Object>) JsonPath.read(after, "$.trend")).hasSize(12 * Category.values().length);

        mockMvc.perform(get("/api/market"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[*].category", hasItem("SPORT")))
                .andExpect(jsonPath("$.heatmap.bids.length()").value(7))
                .andExpect(jsonPath("$.heatmap.bids[0].length()").value(24));
    }

    private String market() throws Exception {
        marketStatsService.clearCache();
        return mockMvc.perform(get("/api/market")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static long heatSum(String json) {
        List<List<Number>> grid = JsonPath.read(json, "$.heatmap.bids");
        return grid.stream().flatMap(List::stream).mapToLong(Number::longValue).sum();
    }

    private User user(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }
}
