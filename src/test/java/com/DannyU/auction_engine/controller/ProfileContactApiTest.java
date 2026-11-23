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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste pentru profilul public al vanzatorului si datele de contact dupa vanzare
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileContactApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void profilulPublicArataObiecteleActiveSiCeleVanduteFaraEmail() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        createItem(seller);
        Item vandut = createItem(seller);
        vandut.setActive(false);
        vandut.setWinner(buyer);
        Item nevandut = createItem(seller);
        nevandut.setActive(false);

        mockMvc.perform(get("/api/users/" + seller.getUsername() + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(seller.getUsername()))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.activeItems.length()").value(1))
                .andExpect(jsonPath("$.soldItems.length()").value(1))
                .andExpect(jsonPath("$.soldItems[0].id").value(vandut.getId()))
                .andExpect(jsonPath("$.soldItems[0].winner.email").doesNotExist());

        mockMvc.perform(get("/api/users/nu_exista_deloc/profile"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Utilizatorul nu a fost gasit!"));
    }

    @Test
    void vanzatorulSiCastigatorulIsiVadEmailurileDupaVanzare() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = createItem(seller);
        item.setActive(false);
        item.setWinner(buyer);

        contact(item, seller)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("castigator"))
                .andExpect(jsonPath("$.username").value(buyer.getUsername()))
                .andExpect(jsonPath("$.email").value(buyer.getEmail()));
        contact(item, buyer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("vanzator"))
                .andExpect(jsonPath("$.email").value(seller.getEmail()));
    }

    @Test
    void altiiNuVadDateleDeContact() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        User altul = createUser("altul");
        Item item = createItem(seller);

        // Licitatie inca deschisa
        contact(item, seller)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Datele de contact apar doar dupa ce obiectul a fost vandut!"));

        item.setActive(false);
        item.setWinner(buyer);
        contact(item, altul)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Doar vanzatorul si castigatorul pot vedea datele de contact!"));
        mockMvc.perform(get("/api/items/" + item.getId() + "/contact")).andExpect(status().isUnauthorized());
    }

    // ---------- Metode ajutatoare ----------

    private org.springframework.test.web.servlet.ResultActions contact(Item item, User user) throws Exception {
        return mockMvc.perform(get("/api/items/" + item.getId() + "/contact").header("Authorization", bearer(user)));
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
