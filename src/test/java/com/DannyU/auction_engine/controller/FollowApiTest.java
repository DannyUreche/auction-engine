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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Following sellers: idempotent follow, follower count, notification on a new listing
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FollowApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void followersHearAboutNewListings() throws Exception {
        User seller = user("vanzator");
        User fan = user("fan");
        String fanToken = bearer(fan);
        String url = "/api/users/" + seller.getUsername() + "/follow";

        mockMvc.perform(post("/api/users/" + fan.getUsername() + "/follow").header("Authorization", fanToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(url).header("Authorization", fanToken)).andExpect(jsonPath("$.following").value(true));
        mockMvc.perform(post(url).header("Authorization", fanToken)).andExpect(jsonPath("$.followers").value(1));
        mockMvc.perform(get("/api/users/" + seller.getUsername() + "/profile")).andExpect(jsonPath("$.followers").value(1));
        mockMvc.perform(get("/api/users/me/following").header("Authorization", fanToken))
                .andExpect(jsonPath("$[0].username").value(seller.getUsername()));

        mockMvc.perform(post("/api/items").header("Authorization", bearer(seller)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Biblioteca veche\", \"startPrice\": 200, \"durationInMinutes\": 60}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications").header("Authorization", fanToken))
                .andExpect(jsonPath("$.items[0].type").value("NEW_LISTING"))
                .andExpect(jsonPath("$.items[0].message", containsString("Biblioteca veche")));

        mockMvc.perform(delete(url).header("Authorization", fanToken)).andExpect(jsonPath("$.following").value(false));
        mockMvc.perform(get(url).header("Authorization", fanToken)).andExpect(jsonPath("$.followers").value(0));
        mockMvc.perform(post(url)).andExpect(status().isUnauthorized());
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
}
