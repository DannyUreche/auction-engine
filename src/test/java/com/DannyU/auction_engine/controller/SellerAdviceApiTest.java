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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sell form advice: similar sales give an estimate, a suggested start price and the monthly history
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SellerAdviceApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BidRepository bidRepository;

    @Test
    void similarSalesBecomeAdvice() throws Exception {
        User seller = user("sfat_v");
        User buyer = user("sfat_c");
        String word = "Zorblax" + UUID.randomUUID().toString().substring(0, 6).replaceAll("[^a-z]", "q");
        for (String price : new String[]{"1000", "1200", "1400"}) {
            Item item = new Item();
            item.setName("Statueta " + word + " de colectie");
            item.setStartPrice(new BigDecimal("100"));
            item.setCurrentPrice(new BigDecimal(price));
            item.setSeller(seller);
            item.setCategory(Category.ARTA_COLECTII);
            item.setEndTime(LocalDateTime.now().minusHours(1));
            item.setActive(false);
            item.setWinner(buyer);
            item = itemRepository.save(item);
            Bid bid = new Bid();
            bid.setItem(item);
            bid.setBidder(buyer);
            bid.setAmount(new BigDecimal(price));
            bidRepository.save(bid);
        }

        mockMvc.perform(get("/api/items/price-advice").param("name", "Vand statueta " + word).param("category", "ARTA_COLECTII"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimate.basis").value("SIMILAR"))
                .andExpect(jsonPath("$.estimate.median").value(1200))
                .andExpect(jsonPath("$.suggestedStart").value(720))
                .andExpect(jsonPath("$.suggestedBuyNow").value(1450))
                .andExpect(jsonPath("$.history.length()").value(6))
                .andExpect(jsonPath("$.history[5].sales").isNumber())
                .andExpect(jsonPath("$.avgBids").isNumber());
        mockMvc.perform(get("/api/items/price-advice").param("category", "ARTA_COLECTII"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimate").doesNotExist());
        mockMvc.perform(get("/api/items/price-advice").param("category", "NIMIC"))
                .andExpect(status().isBadRequest());
    }

    private User user(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }
}
