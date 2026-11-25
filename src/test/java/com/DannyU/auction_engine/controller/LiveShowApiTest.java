package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import com.jayway.jsonpath.JsonPath;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Live shows: items get consecutive slots, rules for what can go on stage
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LiveShowApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void itemsGetConsecutiveSlots() throws Exception {
        User seller = user("vanzator");
        User fan = user("fan");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/users/" + seller.getUsername() + "/follow")
                .header("Authorization", bearer(fan))).andExpect(status().isOk());
        Item a = item(seller);
        Item b = item(seller);
        Item c = item(seller);
        LocalDateTime start = LocalDateTime.now().plusMinutes(10).truncatedTo(ChronoUnit.SECONDS);

        mockMvc.perform(get("/api/shows/eligible-items").header("Authorization", bearer(seller)))
                .andExpect(jsonPath("$.length()").value(3));
        create(seller, "{\"title\": \"Seara\", \"slotMinutes\": 3, \"itemIds\": [" + a.getId() + "]}").andExpect(status().isBadRequest());
        create(user("altul"), "{\"slotMinutes\": 3, \"itemIds\": [" + a.getId() + ", " + b.getId() + "]}").andExpect(status().isBadRequest());

        String body = create(seller, "{\"title\": \"Seara de colectie\", \"startTime\": \"" + start + "\", \"slotMinutes\": 3, \"itemIds\": ["
                + c.getId() + ", " + a.getId() + ", " + b.getId() + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.show.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.items[0].id").value(c.getId()))
                .andReturn().getResponse().getContentAsString();
        Integer showId = JsonPath.read(body, "$.show.id");

        Item second = itemRepository.findById(a.getId()).orElseThrow();
        assertThat(second.getStartTime()).isEqualTo(start.plusMinutes(3));
        assertThat(second.getEndTime()).isEqualTo(start.plusMinutes(6));
        assertThat(second.isStarted()).isFalse();

        // A late bid on a live lot adds 15 s and pushes the next lots by the same amount
        Item first = itemRepository.findById(c.getId()).orElseThrow();
        first.setStartTime(LocalDateTime.now().minusMinutes(2));
        first.setEndTime(LocalDateTime.now().plusSeconds(5));
        itemRepository.save(first);
        LocalDateTime secondStartBefore = itemRepository.findById(a.getId()).orElseThrow().getStartTime();
        User bidder = user("licitator");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/bids")
                        .header("Authorization", bearer(bidder)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": " + c.getId() + ", \"amount\": 105}"))
                .andExpect(status().isOk());
        Item afterBid = itemRepository.findById(c.getId()).orElseThrow();
        assertThat(afterBid.getEndTime()).isBetween(LocalDateTime.now().plusSeconds(13), LocalDateTime.now().plusSeconds(16));
        assertThat(itemRepository.findById(a.getId()).orElseThrow().getStartTime()).isAfter(secondStartBefore);

        mockMvc.perform(get("/api/shows/" + showId))
                .andExpect(jsonPath("$.show.title").value("Seara de colectie"))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[2].liveShowId").value(showId));
        mockMvc.perform(get("/api/shows")).andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(fan)))
                .andExpect(jsonPath("$.items[0].type").value("LIVE_SHOW"));
        // Already on stage: can't be used again
        create(seller, "{\"slotMinutes\": 3, \"itemIds\": [" + a.getId() + ", " + b.getId() + "]}").andExpect(status().isBadRequest());
    }

    private ResultActions create(User user, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/shows")
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

    private Item item(User seller) {
        Item item = new Item();
        item.setName("Obiect pe scena");
        item.setStartPrice(new BigDecimal("100"));
        item.setCurrentPrice(new BigDecimal("100"));
        item.setSeller(seller);
        item.setCategory(Category.ALTELE);
        item.setEndTime(LocalDateTime.now().plusDays(1));
        return itemRepository.save(item);
    }
}
