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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste pentru editarea unui obiect de catre vanzator; tranzactia e anulata la final
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemEditApiTest {

    private static final String EDITARE = """
            {"name": "Ceas nou", "description": "Descriere noua", "category": "CEASURI_BIJUTERII",
             "startPrice": 150, "buyNowPrice": 400}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void vanzatorulModificaObiectulFaraOferte() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);
        LocalDateTime endTime = item.getEndTime();

        edit(item, seller, EDITARE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ceas nou"))
                .andExpect(jsonPath("$.description").value("Descriere noua"))
                .andExpect(jsonPath("$.category").value("CEASURI_BIJUTERII"))
                .andExpect(jsonPath("$.startPrice").value(150))
                .andExpect(jsonPath("$.currentPrice").value(150))
                .andExpect(jsonPath("$.buyNowPrice").value(400));

        // Data de incheiere nu se schimba
        org.assertj.core.api.Assertions.assertThat(item.getEndTime()).isEqualTo(endTime);
    }

    @Test
    void dupaPrimaOfertaObiectulNuMaiPoateFiModificat() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = createItem(seller);

        mockMvc.perform(post("/api/bids").header("Authorization", bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": " + item.getId() + ", \"amount\": 110}"))
                .andExpect(status().isOk());

        edit(item, seller, EDITARE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Obiectul are deja oferte si nu mai poate fi modificat!"));
    }

    @Test
    void doarVanzatorulPoateModificaSiDoarCatLicitatiaEDeschisa() throws Exception {
        User seller = createUser("vanzator");
        User altul = createUser("altul");
        Item item = createItem(seller);

        edit(item, altul, EDITARE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Doar vanzatorul poate modifica obiectul!"));

        item.setActive(false);
        edit(item, seller, EDITARE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Licitatia s-a incheiat, obiectul nu mai poate fi modificat!"));

        mockMvc.perform(put("/api/items/" + item.getId()).contentType(MediaType.APPLICATION_JSON).content(EDITARE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dateleInvalideSuntRespinse() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller);

        edit(item, seller, "{\"name\": \"\", \"startPrice\": 100}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Numele obiectului este obligatoriu!"));
        edit(item, seller, "{\"name\": \"X\", \"startPrice\": 100, \"buyNowPrice\": 50}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Pretul \"Cumpara acum\" trebuie sa fie mai mare decat pretul de pornire!"));
    }

    // ---------- Metode ajutatoare ----------

    private ResultActions edit(Item item, User user, String json) throws Exception {
        return mockMvc.perform(put("/api/items/" + item.getId())
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
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
