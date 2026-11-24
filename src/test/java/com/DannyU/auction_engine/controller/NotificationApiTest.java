package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AuctionScheduler;
import com.DannyU.auction_engine.service.JwtService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste pentru notificari (clopotelul): oferta depasita, oferta noua, castigat / vandut
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private AuctionScheduler auctionScheduler;

    @Test
    void notificariPentruOfertaDepasitaSiOfertaNoua() throws Exception {
        User seller = createUser("vanzator");
        User ana = createUser("ana");
        User ion = createUser("ion");
        Item item = createItem(seller);

        bid(item, ana, 110).andExpect(status().isOk());
        bid(item, ion, 150).andExpect(status().isOk());

        notifications(ana)
                .andExpect(jsonPath("$.unread").value(1))
                .andExpect(jsonPath("$.items[0].type").value("OUTBID"))
                .andExpect(jsonPath("$.items[0].itemId").value(item.getId()))
                .andExpect(jsonPath("$.items[0].read").value(false));
        notifications(seller)
                .andExpect(jsonPath("$.unread").value(2))
                .andExpect(jsonPath("$.items[0].type").value("NEW_BID"));
        notifications(ion).andExpect(jsonPath("$.unread").value(0));

        mockMvc.perform(post("/api/notifications/read-all").header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(0))
                .andExpect(jsonPath("$.items[0].read").value(true));
    }

    @Test
    void laInchidereCastigatorulSiVanzatorulSuntAnuntati() throws Exception {
        User seller = createUser("vanzator");
        User ana = createUser("ana");
        Item item = createItem(seller);
        bid(item, ana, 120).andExpect(status().isOk());

        auctionScheduler.closeAuction(item);

        notifications(ana).andExpect(jsonPath("$.items[0].type").value("WON"));
        notifications(seller).andExpect(jsonPath("$.items[0].type").value("SOLD"));
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
    }

    // ---------- Metode ajutatoare ----------

    private ResultActions notifications(User user) throws Exception {
        return mockMvc.perform(get("/api/notifications").header("Authorization", bearer(user)));
    }

    private ResultActions bid(Item item, User user, int amount) throws Exception {
        return mockMvc.perform(post("/api/bids").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\": " + item.getId() + ", \"amount\": " + amount + "}"));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
    }

    private User createUser(String prefix) {
        String unic = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(unic);
        user.setEmail(unic + "@test.ro");
        user.setPassword("parola-test");
        return userRepository.save(user);
    }

    private Item createItem(User owner) {
        Item newItem = new Item();
        newItem.setName("Obiect test");
        newItem.setStartPrice(new BigDecimal("100.00"));
        newItem.setCurrentPrice(new BigDecimal("100.00"));
        newItem.setSeller(owner);
        newItem.setEndTime(LocalDateTime.now().plusHours(1));
        return itemRepository.save(newItem);
    }
}
