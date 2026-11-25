package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AiModerationService;
import com.DannyU.auction_engine.service.JwtService;
import com.DannyU.auction_engine.service.ListingModerator;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AI moderation: suspicious new listings become "Asistent AI" reports (Claude is mocked)
@SpringBootTest(properties = "app.admin-usernames=" + AiModerationApiTest.ADMIN)
@AutoConfigureMockMvc
@Transactional
class AiModerationApiTest {

    static final String ADMIN = "admin_test_moderare";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private AiModerationService aiModerationService;
    @MockitoBean
    private ListingModerator moderator;

    @Test
    void suspiciousListingsReachTheAdmins() throws Exception {
        when(moderator.isEnabled()).thenReturn(true);
        when(moderator.review(any())).thenAnswer(call -> {
            Item item = call.getArgument(0);
            return item.getName().contains("pistol")
                    ? new ListingModerator.Verdict(true, "PROHIBITED", "Arma de foc, interzisa pe platforma.")
                    : new ListingModerator.Verdict(false, "NONE", "");
        });
        String seller = "Bearer " + jwtService.generateToken(user("vanzator").getUsername());
        Integer bad = create(seller, "Vand pistol de colectie");
        Integer good = create(seller, "Vand lampa veche");

        for (int i = 0; i < 100 && (!checked(bad) || !checked(good)); i++) {
            aiModerationService.checkNewListings();
        }
        assertThat(checked(bad)).isTrue();
        assertThat(checked(good)).isTrue();

        String admin = "Bearer " + jwtService.generateToken(user(ADMIN, false).getUsername());
        mockMvc.perform(get("/api/admin/reports").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.itemId == " + bad + ")].source", hasItem("AI")))
                .andExpect(jsonPath("$[?(@.itemId == " + bad + ")].reporter", hasItem("Asistent AI")))
                .andExpect(jsonPath("$[?(@.itemId == " + bad + ")].reason", hasItem("PROHIBITED")))
                .andExpect(jsonPath("$[*].itemId", not(hasItem(good))));
        mockMvc.perform(get("/api/admin/ai").header("Authorization", admin)).andExpect(status().isOk());
    }

    private boolean checked(Integer id) {
        return itemRepository.findById(id).orElseThrow().isAiChecked();
    }

    private Integer create(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/items").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\", \"startPrice\": 100, \"durationInMinutes\": 60}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private User user(String name) {
        return user(name, true);
    }

    private User user(String name, boolean unique) {
        String username = unique ? name + "_" + UUID.randomUUID().toString().substring(0, 8) : name;
        User user = userRepository.findByUsername(username).orElseGet(User::new);
        user.setUsername(username);
        user.setEmail(username + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }
}
