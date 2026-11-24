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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste pentru licitarea automata (proxy bidding); tranzactia e anulata la final
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AutoBidApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void licitareaAutomataRaspundeLaOfertaManuala() throws Exception {
        User seller = createUser("vanzator");
        User ana = createUser("ana");
        User ion = createUser("ion");
        Item item = createItem(seller);

        // Ana seteaza maxim 300; nimeni nu licita, deci ea liciteaza imediat pretul minim (100 + pasul de 5)
        autoBid(item, ana, 300).andExpect(status().isOk()).andExpect(jsonPath("$.maxAmount").value(300));
        price(item, 105);

        // Ion ofera 150 manual -> licitarea Anei raspunde cu 155 (pasul la 150 e 5)
        bid(item, ion, 150).andExpect(status().isOk());
        price(item, 155);
        mockMvc.perform(get("/api/bids/" + item.getId()))
                .andExpect(jsonPath("$[0].bidder.username").value(ana.getUsername()))
                .andExpect(jsonPath("$[0].automatic").value(true))
                .andExpect(jsonPath("$[1].automatic").value(false));

        // Ion trece de maximul Anei -> licitarea ei se opreste
        bid(item, ion, 400).andExpect(status().isOk());
        price(item, 400);
    }

    @Test
    void douaLicitariAutomateCastigaCeaCuMaximulMaiMare() throws Exception {
        User seller = createUser("vanzator");
        User ana = createUser("ana");
        User ion = createUser("ion");
        Item item = createItem(seller);

        autoBid(item, ana, 500).andExpect(status().isOk());
        autoBid(item, ion, 300).andExpect(status().isOk());

        // Ion urca pana la maximul lui (300), Ana raspunde cu 300 + pasul (5) si conduce
        price(item, 305);
        mockMvc.perform(get("/api/bids/" + item.getId()))
                .andExpect(jsonPath("$[0].bidder.username").value(ana.getUsername()))
                .andExpect(jsonPath("$[1].amount").value(300));
    }

    @Test
    void maximulEsteSecretSiPoateFiAnulat() throws Exception {
        User seller = createUser("vanzator");
        User ana = createUser("ana");
        Item item = createItem(seller);

        autoBid(item, ana, 50)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Suma maxima trebuie sa fie cel putin 105.00 RON!"));
        autoBid(item, seller, 500)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Nu poti licita pentru propriul obiect!"));

        autoBid(item, ana, 300).andExpect(status().isOk());
        mockMvc.perform(get("/api/items/" + item.getId() + "/auto-bid").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.maxAmount").value(300));
        // Altcineva (sau un vizitator) nu vede maximul
        mockMvc.perform(get("/api/items/" + item.getId() + "/auto-bid")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/items/" + item.getId() + "/auto-bid").header("Authorization", bearer(seller)))
                .andExpect(jsonPath("$.maxAmount").doesNotExist());

        mockMvc.perform(delete("/api/items/" + item.getId() + "/auto-bid").header("Authorization", bearer(ana)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/items/" + item.getId() + "/auto-bid").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.maxAmount").doesNotExist());
    }

    // ---------- Metode ajutatoare ----------

    private ResultActions autoBid(Item item, User user, int max) throws Exception {
        return mockMvc.perform(put("/api/items/" + item.getId() + "/auto-bid").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"maxAmount\": " + max + "}"));
    }

    private ResultActions bid(Item item, User user, int amount) throws Exception {
        return mockMvc.perform(post("/api/bids").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\": " + item.getId() + ", \"amount\": " + amount + "}"));
    }

    private void price(Item item, int expected) throws Exception {
        mockMvc.perform(get("/api/items/" + item.getId())).andExpect(jsonPath("$.currentPrice").value(expected));
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
