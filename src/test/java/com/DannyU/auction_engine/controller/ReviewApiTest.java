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

// Teste pentru recenziile dintre cumparator si vanzator
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void recenziileAparPeProfilCuMedia() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = soldItem(seller, buyer);

        review(item, buyer, "{\"rating\": 5, \"comment\": \"Totul perfect!\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("cumparator"))
                .andExpect(jsonPath("$.target").value(seller.getUsername()));
        review(item, seller, "{\"rating\": 4}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("vanzator"));

        mockMvc.perform(get("/api/users/" + seller.getUsername() + "/profile"))
                .andExpect(jsonPath("$.rating.average").value(5.0))
                .andExpect(jsonPath("$.rating.count").value(1))
                .andExpect(jsonPath("$.reviews[0].comment").value("Totul perfect!"))
                .andExpect(jsonPath("$.reviews[0].author").value(buyer.getUsername()));
        mockMvc.perform(get("/api/items/" + item.getId() + "/reviews"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void reguliPentruRecenzii() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        User altul = createUser("altul");
        Item item = soldItem(seller, buyer);

        review(item, altul, "{\"rating\": 1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Doar vanzatorul si castigatorul pot lasa recenzii pentru acest obiect!"));
        review(item, buyer, "{\"rating\": 6}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Nota trebuie sa fie intre 1 si 5 stele!"));
        review(item, buyer, "{\"rating\": 3}").andExpect(status().isOk());
        review(item, buyer, "{\"rating\": 5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Ai lasat deja o recenzie pentru acest obiect!"));

        item.setActive(true);
        item.setWinner(null);
        review(item, seller, "{\"rating\": 5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Poti lasa o recenzie doar dupa ce obiectul a fost vandut!"));
        mockMvc.perform(post("/api/items/" + item.getId() + "/reviews").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\": 5}")).andExpect(status().isUnauthorized());
    }

    // ---------- Metode ajutatoare ----------

    private ResultActions review(Item item, User user, String json) throws Exception {
        return mockMvc.perform(post("/api/items/" + item.getId() + "/reviews").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(json));
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
