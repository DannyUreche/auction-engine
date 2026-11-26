package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AuctionScheduler;
import com.DannyU.auction_engine.service.JwtService;
import com.jayway.jsonpath.JsonPath;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Scheduled auctions, Dutch auctions and the virtual wallet (the test transaction is rolled back)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuctionTypesWalletApiTest {

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
    void scheduledAuctionAcceptsBidsOnlyAfterItStarts() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        String start = LocalDateTime.now().plusMinutes(10).truncatedTo(ChronoUnit.SECONDS).toString();

        Integer id = createItem(seller, "{\"name\": \"Programat\", \"startPrice\": 100, \"startTime\": \"" + start + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.started").value(false))
                .andExpect(jsonPath("$.startTime").value(start))
                .andReturn().getResponse().getContentAsString().transform(body -> JsonPath.read(body, "$.id"));

        bid(id, buyer, 110).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Licitatia nu a inceput inca!"));
        createItem(seller, "{\"name\": \"Prea curand\", \"startPrice\": 100, \"startTime\": \"" + LocalDateTime.now() + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Inceputul licitatiei trebuie sa fie intre 1 minut si 30 de zile de acum!"));

        // The start time comes: watchers are notified and bids are accepted
        mockMvc.perform(post("/api/items/" + id + "/watch").header("Authorization", bearer(buyer))).andExpect(status().isOk());
        Item item = itemRepository.findById(id).orElseThrow();
        item.setStartTime(LocalDateTime.now().minusSeconds(1));
        auctionScheduler.updateRunningAuctions();
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(buyer)))
                .andExpect(jsonPath("$.items[0].type").value("STARTED"));
        bid(id, buyer, 110).andExpect(status().isOk());
    }

    @Test
    void dutchAuctionIsBoughtAtTheCurrentPriceAndPaidFromTheWallet() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Integer id = createItem(seller, "{\"name\": \"Olandeza\", \"startPrice\": 1000, \"auctionType\": \"DUTCH\", \"floorPrice\": 400, \"priceDrop\": 50}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionType").value("DUTCH"))
                .andExpect(jsonPath("$.buyNowAvailable").value(true))
                .andReturn().getResponse().getContentAsString().transform(body -> JsonPath.read(body, "$.id"));

        bid(id, buyer, 1100).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("La licitatia olandeza nu se liciteaza: cumperi la pretul curent!"));

        // Three minutes later the price is 1000 - 3 x 50
        Item item = itemRepository.findById(id).orElseThrow();
        item.setStartTime(LocalDateTime.now().minusMinutes(3).minusSeconds(5));
        mockMvc.perform(post("/api/items/" + id + "/buy-now").header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(850));
        assertThat(item.getActive()).isFalse();
        assertThat(item.getWinner().getId()).isEqualTo(buyer.getId());

        // Escrow: the buyer paid, the seller gets the money only after the buyer confirms
        wallet(buyer).andExpect(jsonPath("$.balance").value(9150))
                .andExpect(jsonPath("$.inEscrow").value(850))
                .andExpect(jsonPath("$.transactions[0].type").value("PURCHASE"));
        wallet(seller).andExpect(jsonPath("$.balance").value(10000))
                .andExpect(jsonPath("$.pendingSales").value(850));

        createItem(seller, "{\"name\": \"Gresit\", \"startPrice\": 100, \"auctionType\": \"DUTCH\", \"floorPrice\": 200, \"priceDrop\": 5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Pretul minim trebuie sa fie mai mic decat pretul de pornire!"));
    }

    @Test
    void walletReservesLeadingBidsAndBlocksBidsWithoutMoney() throws Exception {
        User seller = createUser("vanzator");
        User buyer = createUser("cumparator");
        Integer first = JsonPath.read(createItem(seller, "{\"name\": \"A\", \"startPrice\": 100}").andReturn().getResponse().getContentAsString(), "$.id");
        Integer second = JsonPath.read(createItem(seller, "{\"name\": \"B\", \"startPrice\": 100}").andReturn().getResponse().getContentAsString(), "$.id");

        bid(first, buyer, 300).andExpect(status().isOk());
        wallet(buyer).andExpect(jsonPath("$.reserved").value(300)).andExpect(jsonPath("$.available").value(9700));

        bid(second, buyer, 9800).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.eroare").value("Fonduri insuficiente! Ai disponibil 9700.00 RON. Alimenteaza portofelul din Contul meu."));
        // Raising your own leading bid may use the money already reserved on that item
        bid(first, buyer, 9900).andExpect(status().isOk());

        mockMvc.perform(post("/api/wallet/top-up").header("Authorization", bearer(buyer)).contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 6000}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/wallet/top-up").header("Authorization", bearer(buyer)).contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10500))
                .andExpect(jsonPath("$.transactions[0].type").value("TOP_UP"));
        mockMvc.perform(get("/api/wallet")).andExpect(status().isUnauthorized());
    }

    // ---------- Helpers ----------

    private ResultActions createItem(User seller, String json) throws Exception {
        return mockMvc.perform(post("/api/items").header("Authorization", bearer(seller)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions bid(Integer itemId, User user, int amount) throws Exception {
        return mockMvc.perform(post("/api/bids").header("Authorization", bearer(user)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\": " + itemId + ", \"amount\": " + amount + "}"));
    }

    private ResultActions wallet(User user) throws Exception {
        return mockMvc.perform(get("/api/wallet").header("Authorization", bearer(user)));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
    }

    private User createUser(String prefix) {
        String unique = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(unique);
        user.setEmail(unique + "@test.ro");
        user.setPassword("parola-test");
        return userRepository.save(user);
    }
}
