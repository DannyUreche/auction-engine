package com.DannyU.auction_engine.controller;

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

// Teste pentru chat-ul dintre castigator si vanzator
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void castigatorulSiVanzatorulIsiScriuMesaje() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = soldItem(seller, buyer);

        send(item, buyer, "Buna! Cand putem face predarea?").andExpect(status().isOk())
                .andExpect(jsonPath("$.senderUsername").value(buyer.getUsername()))
                .andExpect(jsonPath("$.sender").doesNotExist());
        send(item, seller, "Maine dupa ora 18, in centru.").andExpect(status().isOk());

        mockMvc.perform(get("/api/items/" + item.getId() + "/messages").header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].text").value("Buna! Cand putem face predarea?"))
                .andExpect(jsonPath("$[1].senderUsername").value(seller.getUsername()));
        // Vanzatorul a primit notificare pentru mesajul cumparatorului
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(seller)))
                .andExpect(jsonPath("$.items[0].type").value("MESSAGE"));
    }

    @Test
    void altiiNuPotCitiSauScrie() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        User altul = createUser("altul");
        Item item = soldItem(seller, buyer);

        mockMvc.perform(get("/api/items/" + item.getId() + "/messages").header("Authorization", bearer(altul)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Doar vanzatorul si castigatorul pot vedea acest chat!"));
        mockMvc.perform(get("/api/items/" + item.getId() + "/messages")).andExpect(status().isUnauthorized());
        send(item, buyer, "   ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Mesajul nu poate fi gol!"));

        item.setActive(true);
        item.setWinner(null);
        send(item, seller, "Salut").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Chat-ul este disponibil doar dupa ce obiectul a fost vandut!"));
    }

    // ---------- Metode ajutatoare ----------

    private ResultActions send(Item item, User user, String text) throws Exception {
        return mockMvc.perform(post("/api/items/" + item.getId() + "/messages").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"" + text + "\"}"));
    }

    private Item soldItem(User seller, User buyer) {
        Item item = createItem(seller);
        item.setActive(false);
        item.setWinner(buyer);
        return item;
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
