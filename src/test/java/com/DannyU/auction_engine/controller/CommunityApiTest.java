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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Questions & answers, reports and blocked accounts, recommendations, push subscriptions, link previews
@SpringBootTest(properties = "app.admin-usernames=" + CommunityApiTest.ADMIN)
@AutoConfigureMockMvc
@Transactional
class CommunityApiTest {

    static final String ADMIN = "admin_test_comunitate";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Test
    void questionsAreAskedByBuyersAndAnsweredOnceBySeller() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = createItem(seller, Category.MODA);

        post("/api/items/" + item.getId() + "/questions", seller, "{\"text\": \"Eu?\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Nu poti pune intrebari la propriul obiect!"));
        String body = post("/api/items/" + item.getId() + "/questions", buyer, "{\"text\": \"Are cutia originala?\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorUsername").value(buyer.getUsername()))
                .andReturn().getResponse().getContentAsString();
        Integer questionId = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        post("/api/items/" + item.getId() + "/questions/" + questionId + "/answer", buyer, "{\"text\": \"Da\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Doar vanzatorul poate raspunde la intrebari!"));
        post("/api/items/" + item.getId() + "/questions/" + questionId + "/answer", seller, "{\"text\": \"Da, si factura.\"}")
                .andExpect(status().isOk());
        post("/api/items/" + item.getId() + "/questions/" + questionId + "/answer", seller, "{\"text\": \"Din nou\"}")
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/items/" + item.getId() + "/questions"))
                .andExpect(jsonPath("$[0].text").value("Are cutia originala?"))
                .andExpect(jsonPath("$[0].answer").value("Da, si factura."));
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(buyer)))
                .andExpect(jsonPath("$.items[0].type").value("ANSWER"));
    }

    @Test
    void reportsReachAdminsAndBlockedUsersAreLockedOut() throws Exception {
        User admin = createUser(ADMIN, false);
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item item = createItem(seller, Category.ALTELE);

        post("/api/items/" + item.getId() + "/report", buyer, "{\"reason\": \"FRAUD\", \"details\": \"Poze furate\"}")
                .andExpect(status().isOk());
        post("/api/items/" + item.getId() + "/report", buyer, "{\"reason\": \"OTHER\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Ai raportat deja acest obiect!"));
        mockMvc.perform(get("/api/admin/reports").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[?(@.itemId == " + item.getId() + ")].reason").value("FRAUD"));

        // Blocked: the token stops working and login is refused
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/users/" + seller.getId() + "/block")
                .header("Authorization", bearer(admin))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(seller))).andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/admin/users/" + admin.getId() + "/block")
                .header("Authorization", bearer(admin))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Un administrator nu poate fi blocat!"));
    }

    @Test
    void recommendationsFollowTheCategoriesYouBidOn() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Item watch = createItem(seller, Category.CEASURI_BIJUTERII);
        Item otherWatch = createItem(seller, Category.CEASURI_BIJUTERII);
        post("/api/bids", buyer, "{\"itemId\": " + watch.getId() + ", \"amount\": 110}").andExpect(status().isOk());

        mockMvc.perform(get("/api/items/recommended").header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(otherWatch.getId())))
                .andExpect(jsonPath("$[*].id", not(org.hamcrest.Matchers.hasItem(watch.getId()))));
        mockMvc.perform(get("/api/items/recommended")).andExpect(status().isUnauthorized());
    }

    @Test
    void pushSubscriptionsCanBeSavedAndRemoved() throws Exception {
        User user = createUser("telefon");
        mockMvc.perform(get("/api/push/public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value(org.hamcrest.Matchers.matchesPattern("[A-Za-z0-9_-]{87}")));
        String subscription = "{\"endpoint\": \"https://push.example/" + UUID.randomUUID() + "\", \"keys\": {\"p256dh\": \"cheie\", \"auth\": \"secret\"}}";
        post("/api/push/subscribe", user, subscription).andExpect(status().isNoContent());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/push/subscribe")
                .contentType(MediaType.APPLICATION_JSON).content(subscription)).andExpect(status().isUnauthorized());
    }

    @Test
    void itemPagesHaveLinkPreviewTags() throws Exception {
        User seller = createUser("vanzator");
        Item item = createItem(seller, Category.ELECTRONICE);
        mockMvc.perform(get("/items/" + item.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<meta property=\"og:title\" content=\"Telefon &quot;special&quot; · Licitatii.live\">")))
                .andExpect(content().string(containsString("og:image")));
    }

    // ---------- Helpers ----------

    private ResultActions post(String url, User user, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .header("Authorization", bearer(user)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
    }

    private User createUser(String prefix) {
        return createUser(prefix, true);
    }

    private User createUser(String name, boolean unique) {
        String username = unique ? name + "_" + UUID.randomUUID().toString().substring(0, 8) : name;
        User user = userRepository.findByUsername(username).orElseGet(User::new);
        user.setUsername(username);
        user.setEmail(username + "@test.ro");
        user.setPassword("parola-test");
        return userRepository.save(user);
    }

    private Item createItem(User owner, Category category) {
        Item item = new Item();
        item.setName("Telefon \"special\"");
        item.setStartPrice(new BigDecimal("100.00"));
        item.setCurrentPrice(new BigDecimal("100.00"));
        item.setSeller(owner);
        item.setCategory(category);
        item.setEndTime(LocalDateTime.now().plusHours(1));
        return itemRepository.save(item);
    }
}
