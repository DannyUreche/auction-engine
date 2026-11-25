package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Saved searches: create, list, delete, and notifications when a matching auction opens
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlertApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void matchingNewAuctionNotifiesTheAlertOwner() throws Exception {
        String word = "alerta" + UUID.randomUUID().toString().substring(0, 6);
        User buyer = createUser("cumparator");
        User seller = createUser("vanzator");

        post("/api/alerts", buyer, "{}").andExpect(status().isBadRequest());
        String body = post("/api/alerts", buyer, "{\"query\": \"" + word + "\", \"category\": \"CEASURI_BIJUTERII\", \"maxPrice\": 500}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer alertId = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        mockMvc.perform(get("/api/alerts").header("Authorization", bearer(buyer)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].maxPrice").value(500));

        // Too expensive: no notification; matching: one
        createItem(seller, "Ceas " + word, "900");
        createItem(seller, "Ceas vechi " + word, "300");
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(buyer)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].type").value("ALERT"))
                .andExpect(jsonPath("$.items[0].message", containsString("Ceas vechi " + word)));

        mockMvc.perform(delete("/api/alerts/" + alertId).header("Authorization", bearer(seller)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/alerts/" + alertId).header("Authorization", bearer(buyer)))
                .andExpect(status().isNoContent());
    }

    @Test
    void atMostTenAlerts() throws Exception {
        User user = createUser("colectionar");
        for (int i = 0; i < 10; i++) {
            post("/api/alerts", user, "{\"query\": \"ceas " + i + "\"}").andExpect(status().isOk());
        }
        post("/api/alerts", user, "{\"query\": \"inca unul\"}").andExpect(status().isBadRequest());
    }

    private void createItem(User seller, String name, String price) throws Exception {
        post("/api/items", seller, "{\"name\": \"" + name + "\", \"description\": \"Test\", \"startPrice\": " + price
                + ", \"durationInMinutes\": 60, \"category\": \"CEASURI_BIJUTERII\"}").andExpect(status().isOk());
    }

    private ResultActions post(String url, User user, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .header("Authorization", bearer(user)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
    }

    private User createUser(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }
}
