package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AuctionEvent;
import com.DannyU.auction_engine.service.BidService;
import com.DannyU.auction_engine.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pagina de administrare: acces doar pentru admini, statistici, inchidere si stergere obiecte
@SpringBootTest(properties = "app.admin-usernames=" + AdminApiTest.ADMIN)
@AutoConfigureMockMvc
@Transactional
@RecordApplicationEvents
class AdminApiTest {

    static final String ADMIN = "admin_test_principal";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private BidService bidService;
    @Autowired
    private ApplicationEvents events;

    private User admin;
    private User seller;
    private User bidder;

    @BeforeEach
    void setUp() {
        admin = createUser(ADMIN);
        seller = createUser("vanzator_" + UUID.randomUUID().toString().substring(0, 8));
        bidder = createUser("ofertant_" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Test
    void doarAdminulAreAcces() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/stats").header("Authorization", bearer(seller)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.eroare").value("Nu ai drepturi de administrator!"));
        mockMvc.perform(delete("/api/admin/items/1").header("Authorization", bearer(seller)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stats").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void profilulSpuneCineEAdmin() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.admin").value(true))
                .andExpect(jsonPath("$.email").value(admin.getEmail()));
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(seller)))
                .andExpect(jsonPath("$.admin").value(false));
    }

    @Test
    void statisticileSiListeleContinDateleNoi() throws Exception {
        Item item = createItem(seller);
        bidService.placeBid(item.getId(), bidder.getUsername(), new BigDecimal("150.00"));

        mockMvc.perform(get("/api/admin/stats").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.activeItems").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == '%s')].itemsCount".formatted(seller.getUsername())).value(1))
                .andExpect(jsonPath("$[?(@.username == '%s')].bidsCount".formatted(bidder.getUsername())).value(1))
                .andExpect(jsonPath("$[?(@.username == '%s')].email".formatted(bidder.getUsername())).value(bidder.getEmail()))
                .andExpect(jsonPath("$[?(@.username == '%s')].admin".formatted(ADMIN)).value(true));

        mockMvc.perform(get("/api/admin/items").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(item.getId()))
                .andExpect(jsonPath("$[0].seller").value(seller.getUsername()))
                .andExpect(jsonPath("$[0].bidsCount").value(1));
    }

    @Test
    void adminulInchideLicitatiaImediat() throws Exception {
        Item item = createItem(seller);
        bidService.placeBid(item.getId(), bidder.getUsername(), new BigDecimal("150.00"));

        mockMvc.perform(post("/api/admin/items/" + item.getId() + "/close").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.winner").value(bidder.getUsername()));

        assertThat(item.getActive()).isFalse();
        assertThat(item.getWinner().getId()).isEqualTo(bidder.getId());

        // A doua oara nu se mai poate
        mockMvc.perform(post("/api/admin/items/" + item.getId() + "/close").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Licitatia este deja inchisa!"));
    }

    @Test
    void adminulStergeObiectulCuTotCuOferte() throws Exception {
        Item item = createItem(seller);
        bidService.placeBid(item.getId(), bidder.getUsername(), new BigDecimal("150.00"));
        Integer itemId = item.getId();

        mockMvc.perform(delete("/api/admin/items/" + itemId).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        assertThat(itemRepository.findById(itemId)).isEmpty();
        assertThat(bidRepository.countByItemId(itemId)).isZero();
        assertThat(events.stream(AuctionEvent.class)).contains(AuctionEvent.deleted(itemId));

        mockMvc.perform(delete("/api/admin/items/" + itemId).header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Obiectul nu a fost gasit!"));
    }

    @Test
    void graficeleAuDateleCorecte() throws Exception {
        mockMvc.perform(get("/api/admin/charts").header("Authorization", bearer(seller))).andExpect(status().isForbidden());
        removeExistingDemoData();
        mockMvc.perform(post("/api/admin/demo-data").header("Authorization", bearer(admin))).andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/charts").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidsPerDay.length()").value(14))
                .andExpect(jsonPath("$.salesByCategory[0].label").isNotEmpty())
                .andExpect(jsonPath("$.salesByCategory[0].value").isNumber())
                .andExpect(jsonPath("$.topSellers.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.topSellers[0].username").isNotEmpty());
    }

    @Test
    void adminulGenereazaSiStergeDateleDemo() throws Exception {
        removeExistingDemoData();
        mockMvc.perform(post("/api/admin/demo-data").header("Authorization", bearer(seller)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/demo-data").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(106));
        // The catalog fills every category with at least 10 listings, each with photos and a description
        for (Category category : Category.values()) {
            mockMvc.perform(get("/api/items/active"))
                    .andExpect(jsonPath("$[?(@.category == '" + category.name() + "' && @.seller.username =~ /demo_.*/)]",
                            hasSize(greaterThanOrEqualTo(10))));
        }
        mockMvc.perform(get("/api/items/active"))
                .andExpect(jsonPath("$[?(@.seller.username =~ /demo_.*/ && @.imageUrl == null)]", hasSize(0)))
                .andExpect(jsonPath("$[?(@.auctionType == 'DUTCH' && @.seller.username =~ /demo_.*/)]", hasSize(9)));
        mockMvc.perform(get("/api/users/demo_ana/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist());
        mockMvc.perform(get("/api/admin/demo-data").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.exists").value(true));
        // A doua oara nu se dubleaza
        mockMvc.perform(post("/api/admin/demo-data").header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/admin/demo-data").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(106));
        mockMvc.perform(get("/api/admin/demo-data").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.exists").value(false));
        mockMvc.perform(get("/api/users/demo_ana/profile")).andExpect(status().isBadRequest());
    }

    // ---------- Metode ajutatoare ----------

    // The local database may already contain demo data (e.g. generated for a presentation).
    // Remove it inside the test transaction; the rollback at the end brings it back.
    private void removeExistingDemoData() throws Exception {
        mockMvc.perform(delete("/api/admin/demo-data").header("Authorization", bearer(admin))).andExpect(status().isOk());
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.ro");
        user.setPassword("parola-test");
        return userRepository.save(user);
    }

    private Item createItem(User owner) {
        Item newItem = new Item();
        newItem.setName("Obiect admin test");
        newItem.setStartPrice(new BigDecimal("100.00"));
        newItem.setCurrentPrice(new BigDecimal("100.00"));
        newItem.setSeller(owner);
        newItem.setEndTime(LocalDateTime.now().plusHours(1));
        return itemRepository.save(newItem);
    }
}
