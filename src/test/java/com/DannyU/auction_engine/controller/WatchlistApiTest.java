package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AdminService;
import com.DannyU.auction_engine.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste pentru lista de favorite ("Urmaresc"); tranzactia e anulata la final
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WatchlistApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private AdminService adminService;

    @Test
    void utilizatorulUrmaresteSiApoiNuMaiUrmaresteUnObiect() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = createItem(seller, "Ceas vechi");

        watch(item, buyer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(item.getId()))
                .andExpect(jsonPath("$.watching").value(true));
        // A doua oara nu se dubleaza
        watch(item, buyer).andExpect(status().isOk());

        watchedBy(buyer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Ceas vechi"))
                .andExpect(jsonPath("$[0].seller.email").doesNotExist());

        mockMvc.perform(delete("/api/items/" + item.getId() + "/watch").header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watching").value(false));
        watchedBy(buyer).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listaEstePersonalaSiCeleMaiNoiSuntPrimele() throws Exception {
        User seller = createUser("vanzator");
        User ana = createUser("ana");
        User ion = createUser("ion");
        Item primul = createItem(seller, "Primul");
        Item alDoilea = createItem(seller, "Al doilea");

        watch(primul, ana).andExpect(status().isOk());
        watch(alDoilea, ana).andExpect(status().isOk());

        watchedBy(ana)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Al doilea"))
                .andExpect(jsonPath("$[1].name").value("Primul"));
        watchedBy(ion).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void nuPotiUrmariPropriulObiectSauUnObiectInexistent() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller, "Obiect");

        watch(item, seller)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Nu poti urmari propriul obiect!"));

        mockMvc.perform(post("/api/items/999999/watch").header("Authorization", bearer(seller)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Obiectul nu a fost gasit!"));
    }

    @Test
    void listaDeFavoriteCereAutentificare() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller, "Obiect");

        mockMvc.perform(get("/api/items/watched")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/items/" + item.getId() + "/watch")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/items/" + item.getId() + "/watch")).andExpect(status().isUnauthorized());
    }

    @Test
    void obiectulStersDeAdminDisparesiDinFavorite() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = createItem(seller, "Obiect");
        watch(item, buyer).andExpect(status().isOk());

        adminService.deleteItem(item.getId());

        watchedBy(buyer).andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- Metode ajutatoare ----------

    private ResultActions watch(Item item, User user) throws Exception {
        return mockMvc.perform(post("/api/items/" + item.getId() + "/watch").header("Authorization", bearer(user)));
    }

    private ResultActions watchedBy(User user) throws Exception {
        return mockMvc.perform(get("/api/items/watched").header("Authorization", bearer(user)));
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

    private Item createItem(User owner, String name) {
        Item newItem = new Item();
        newItem.setName(name);
        newItem.setStartPrice(new BigDecimal("100.00"));
        newItem.setCurrentPrice(new BigDecimal("100.00"));
        newItem.setSeller(owner);
        newItem.setEndTime(LocalDateTime.now().plusHours(1));
        return itemRepository.save(newItem);
    }
}
