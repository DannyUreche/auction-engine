package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Private offers: make, counter, accept -> sold; rules
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OfferApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void counterOfferAcceptedByTheBuyerSellsTheItem() throws Exception {
        User seller = user("vanzator");
        User buyer = user("cumparator");
        Item item = item(seller, "500");

        post("/api/items/" + item.getId() + "/offers", seller, "{\"amount\": 300}").andExpect(status().isBadRequest());
        post("/api/items/" + item.getId() + "/offers", buyer, "{\"amount\": 500}").andExpect(status().isBadRequest());
        String body = post("/api/items/" + item.getId() + "/offers", buyer, "{\"amount\": 300, \"message\": \"Ramburs?\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        Integer offerId = JsonPath.read(body, "$.id");
        post("/api/items/" + item.getId() + "/offers", buyer, "{\"amount\": 320}").andExpect(status().isBadRequest());

        // Seller sees it; the buyer can't accept their own offer
        mockMvc.perform(get("/api/items/" + item.getId() + "/offers").header("Authorization", bearer(seller)))
                .andExpect(jsonPath("$[0].buyerUsername").value(buyer.getUsername()))
                .andExpect(jsonPath("$[0].message").value("Ramburs?"));
        reply(offerId, buyer, "ACCEPT", null).andExpect(status().isBadRequest());

        reply(offerId, seller, "COUNTER", "250").andExpect(status().isBadRequest());
        reply(offerId, seller, "COUNTER", "380").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COUNTERED"));
        reply(offerId, buyer, "ACCEPT", null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));

        entityManager.flush();
        entityManager.clear();
        Item sold = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(sold.getActive()).isFalse();
        assertThat(sold.getWinner().getUsername()).isEqualTo(buyer.getUsername());
        assertThat(sold.getCurrentPrice()).isEqualByComparingTo("380");
    }

    @Test
    void aRealBidEndsTheNegotiation() throws Exception {
        User seller = user("vanzator");
        User buyer = user("cumparator");
        User bidder = user("ofertant");
        Item item = item(seller, null);
        String body = post("/api/items/" + item.getId() + "/offers", buyer, "{\"amount\": 90}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer offerId = JsonPath.read(body, "$.id");

        post("/api/bids", bidder, "{\"itemId\": " + item.getId() + ", \"amount\": 110}").andExpect(status().isOk());
        entityManager.clear();
        mockMvc.perform(get("/api/items/" + item.getId() + "/offers").header("Authorization", bearer(buyer)))
                .andExpect(jsonPath("$[0].status").value("EXPIRED"));
        reply(offerId, seller, "ACCEPT", null).andExpect(status().isBadRequest());
    }

    private ResultActions reply(Integer offerId, User user, String action, String counter) throws Exception {
        return post("/api/offers/" + offerId + "/reply", user,
                "{\"action\": \"" + action + "\"" + (counter == null ? "" : ", \"counterAmount\": " + counter) + "}");
    }

    private ResultActions post(String url, User user, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .header("Authorization", bearer(user)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
    }

    private User user(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }

    private Item item(User seller, String buyNow) {
        Item item = new Item();
        item.setName("Obiect negociabil");
        item.setStartPrice(new BigDecimal("100"));
        item.setCurrentPrice(new BigDecimal("100"));
        item.setBuyNowPrice(buyNow == null ? null : new BigDecimal(buyNow));
        item.setSeller(seller);
        item.setCategory(Category.ALTELE);
        item.setEndTime(LocalDateTime.now().plusDays(1));
        return itemRepository.save(item);
    }
}
