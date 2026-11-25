package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AuctionEvent;
import com.DannyU.auction_engine.service.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Live auction room: viewers, view counter and the public live chat
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class LiveRoomApiTest {

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
    @Autowired
    private ApplicationEvents events;

    @Test
    void heartbeatsCountViewersAndVisits() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);

        presence(item, "tab-a", null).andExpect(status().isOk()).andExpect(jsonPath("$.viewers").value(1));
        presence(item, "tab-a", null).andExpect(jsonPath("$.viewers").value(1));
        presence(item, "tab-b", null).andExpect(jsonPath("$.viewers").value(2));
        presence(item, "tab-seller", seller).andExpect(jsonPath("$.viewers").value(3));

        // Two visitors counted; the seller's own visit is not
        entityManager.clear();
        assertThat(itemRepository.findById(item.getId()).orElseThrow().getViewCount()).isEqualTo(2);

        mockMvc.perform(post("/api/items/" + item.getId() + "/presence/leave").contentType(MediaType.APPLICATION_JSON)
                .content("{\"session\": \"tab-b\"}")).andExpect(status().isNoContent());
        presence(item, "tab-a", null).andExpect(jsonPath("$.viewers").value(2));
    }

    @Test
    void liveChatNeedsAnAccountAndAnOpenAuction() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = createItem(seller);

        mockMvc.perform(post("/api/items/" + item.getId() + "/comments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"Salut\"}")).andExpect(status().isUnauthorized());
        comment(item, buyer, "Ce stare are?").andExpect(status().isOk())
                .andExpect(jsonPath("$.authorUsername").value(buyer.getUsername()));
        comment(item, buyer, "Inca o data").andExpect(status().isBadRequest());
        comment(item, seller, "Ca noua!").andExpect(status().isOk());
        assertThat(events.stream(AuctionEvent.class).filter(e -> e.type().equals("comment")).count()).isEqualTo(2);

        mockMvc.perform(get("/api/items/" + item.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Ce stare are?"))
                .andExpect(jsonPath("$[1].text").value("Ca noua!"));

        item.setActive(false);
        itemRepository.save(item);
        comment(item, createUser("altul"), "Prea tarziu").andExpect(status().isBadRequest());
    }

    private ResultActions presence(Item item, String session, User user) throws Exception {
        var request = post("/api/items/" + item.getId() + "/presence").contentType(MediaType.APPLICATION_JSON)
                .content("{\"session\": \"" + session + "\"}");
        if (user != null) {
            request.header("Authorization", "Bearer " + jwtService.generateToken(user.getUsername()));
        }
        return mockMvc.perform(request);
    }

    private ResultActions comment(Item item, User user, String text) throws Exception {
        return mockMvc.perform(post("/api/items/" + item.getId() + "/comments")
                .header("Authorization", "Bearer " + jwtService.generateToken(user.getUsername()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"" + text + "\"}"));
    }

    private User createUser(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }

    private Item createItem(User owner) {
        Item item = new Item();
        item.setName("Obiect live");
        item.setStartPrice(new BigDecimal("100"));
        item.setCurrentPrice(new BigDecimal("100"));
        item.setSeller(owner);
        item.setCategory(Category.ALTELE);
        item.setEndTime(LocalDateTime.now().plusHours(1));
        return itemRepository.save(item);
    }
}
