package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Data export and account deletion (anonymization)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void exportContainsTheUsersOwnData() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = createItem(seller);
        bid(buyer, item, "150");

        mockMvc.perform(get("/api/users/me/export").header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(jsonPath("$.cont.username").value(buyer.getUsername()))
                .andExpect(jsonPath("$.cont.email").value(buyer.getEmail()))
                .andExpect(jsonPath("$.oferte[0].obiect_id").value(item.getId()))
                .andExpect(jsonPath("$.oferte[0].suma").value(150.0));
        mockMvc.perform(get("/api/users/me/export")).andExpect(status().isUnauthorized());
    }

    @Test
    void deletingAnonymizesTheAccountAndRemovesItemsWithoutBids() throws Exception {
        User user = createUser("plecat");
        String username = user.getUsername();
        Item unsold = createItem(user);

        deleteAccount(user, "alt nume").andExpect(status().isBadRequest());
        deleteAccount(user, username).andExpect(status().isOk());

        User anonymized = userRepository.findById(user.getId()).orElseThrow();
        assertThat(anonymized.getUsername()).startsWith("sters_");
        assertThat(anonymized.getEmail()).doesNotContain(username);
        assertThat(anonymized.isBlocked()).isTrue();
        assertThat(userRepository.findByUsername(username)).isEmpty();
        assertThat(itemRepository.findById(unsold.getId())).isEmpty();
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"" + username + "\", \"password\": \"parola-test\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void cannotDeleteWhileLeadingOrSellingWithBids() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = createItem(seller);
        bid(buyer, item, "150");

        deleteAccount(buyer, buyer.getUsername()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Conduci cel putin o licitatie activa. Poti sterge contul dupa ce se incheie."));
        deleteAccount(seller, seller.getUsername()).andExpect(status().isBadRequest());
        assertThat(userRepository.findByUsername(buyer.getUsername())).isPresent();
    }

    // ---------- Helpers ----------

    private org.springframework.test.web.servlet.ResultActions deleteAccount(User user, String confirm) throws Exception {
        return mockMvc.perform(delete("/api/users/me").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirm\": \"" + confirm + "\"}"));
    }

    private void bid(User bidder, Item item, String amount) throws Exception {
        mockMvc.perform(post("/api/bids").header("Authorization", bearer(bidder)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": " + item.getId() + ", \"amount\": " + amount + "}"))
                .andExpect(status().isOk());
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
    }

    private User createUser(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword(passwordEncoder.encode("parola-test"));
        return userRepository.save(user);
    }

    private Item createItem(User owner) {
        Item item = new Item();
        item.setName("Lampa de test");
        item.setStartPrice(new BigDecimal("100.00"));
        item.setCurrentPrice(new BigDecimal("100.00"));
        item.setSeller(owner);
        item.setCategory(Category.ALTELE);
        item.setEndTime(LocalDateTime.now().plusHours(1));
        return itemRepository.save(item);
    }
}
