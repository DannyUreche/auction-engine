package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.PurchaseOrder;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.PurchaseOrderRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.AuctionScheduler;
import com.DannyU.auction_engine.service.BidService;
import com.DannyU.auction_engine.service.JwtService;
import com.DannyU.auction_engine.service.OrderService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Orders with escrow: paid at close, shipped with an AWB, released on confirmation; disputes and deadlines
@SpringBootTest(properties = "app.admin-usernames=" + OrderApiTest.ADMIN)
@AutoConfigureMockMvc
@Transactional
class OrderApiTest {

    static final String ADMIN = "admin_test_comenzi";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private PurchaseOrderRepository orderRepository;
    @Autowired
    private BidService bidService;
    @Autowired
    private AuctionScheduler auctionScheduler;
    @Autowired
    private OrderService orderService;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void happyPathHoldsTheMoneyUntilTheBuyerConfirms() throws Exception {
        User seller = user("cmd_v");
        User buyer = user("cmd_c");
        Integer orderId = sell(seller, buyer, "500");

        wallet(buyer).andExpect(jsonPath("$.balance").value(9500)).andExpect(jsonPath("$.inEscrow").value(500));
        wallet(seller).andExpect(jsonPath("$.balance").value(10000)).andExpect(jsonPath("$.pendingSales").value(500));

        // Only the seller ships, with a known courier and an AWB
        post("/api/orders/" + orderId + "/ship", buyer, "{\"courier\": \"Sameday\", \"awb\": \"1ONB24\"}").andExpect(status().isBadRequest());
        post("/api/orders/" + orderId + "/ship", seller, "{\"courier\": \"Porumbel\", \"awb\": \"1ONB24\"}")
                .andExpect(jsonPath("$.eroare").value("Alege curierul din lista."));
        post("/api/orders/" + orderId + "/ship", seller, "{\"courier\": \"Sameday\", \"awb\": \"1ONB2400123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.autoConfirmAt").isNotEmpty());

        // Strangers see nothing; the buyer sees the order of the item
        Integer itemId = orderRepository.findById(orderId).orElseThrow().getItem().getId();
        mockMvc.perform(get("/api/items/" + itemId + "/order").header("Authorization", bearer(user("altul"))))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/items/" + itemId + "/order").header("Authorization", bearer(buyer)))
                .andExpect(jsonPath("$.role").value("BUYER"))
                .andExpect(jsonPath("$.awb").value("1ONB2400123"));

        post("/api/orders/" + orderId + "/confirm", seller, "{}").andExpect(status().isBadRequest());
        post("/api/orders/" + orderId + "/confirm", buyer, "{}").andExpect(jsonPath("$.status").value("COMPLETED"));
        wallet(seller).andExpect(jsonPath("$.balance").value(10500)).andExpect(jsonPath("$.pendingSales").value(0))
                .andExpect(jsonPath("$.transactions[0].type").value("SALE"));
        wallet(buyer).andExpect(jsonPath("$.inEscrow").value(0));
        mockMvc.perform(get("/api/orders/mine").header("Authorization", bearer(buyer)))
                .andExpect(jsonPath("$.purchases[0].status").value("COMPLETED"));
    }

    @Test
    void disputesAreSettledByAnAdmin() throws Exception {
        User seller = user("cmd_v");
        User buyer = user("cmd_c");
        User admin = user(ADMIN, true);
        Integer orderId = sell(seller, buyer, "300");

        post("/api/orders/" + orderId + "/dispute", buyer, "{\"reason\": \"scurt\"}").andExpect(status().isBadRequest());
        post("/api/orders/" + orderId + "/dispute", buyer, "{\"reason\": \"Vanzatorul nu raspunde la mesaje de o saptamana.\"}")
                .andExpect(jsonPath("$.status").value("DISPUTED"));
        // While in dispute nobody can ship or confirm
        post("/api/orders/" + orderId + "/ship", seller, "{\"courier\": \"Predare personala\"}").andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/disputes").header("Authorization", bearer(seller))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/disputes").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")]").exists());
        post("/api/admin/orders/" + orderId + "/resolve", admin, "{\"refund\": true, \"note\": \"Obiectul nu a fost trimis.\"}")
                .andExpect(jsonPath("$.status").value("REFUNDED"));
        wallet(buyer).andExpect(jsonPath("$.balance").value(10000))
                .andExpect(jsonPath("$.transactions[0].type").value("REFUND"));
        wallet(seller).andExpect(jsonPath("$.balance").value(10000));
    }

    @Test
    void deadlinesProtectBothSides() throws Exception {
        User seller = user("cmd_v");
        User buyer = user("cmd_c");
        PurchaseOrder notShipped = orderRepository.findById(sell(seller, buyer, "200")).orElseThrow();
        PurchaseOrder notConfirmed = orderRepository.findById(sell(seller, buyer, "400")).orElseThrow();
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE orders SET created_at = :at WHERE id = :id")
                .setParameter("at", LocalDateTime.now().minusDays(OrderService.SHIP_DAYS + 1))
                .setParameter("id", notShipped.getId()).executeUpdate();
        post("/api/orders/" + notConfirmed.getId() + "/ship", seller, "{\"courier\": \"DPD\", \"awb\": \"DPD-99887\"}").andExpect(status().isOk());
        notConfirmed.setShippedAt(LocalDateTime.now().minusDays(OrderService.CONFIRM_DAYS + 1));

        orderService.applyDeadlines();
        assertThat(notShipped.getStatus()).isEqualTo(PurchaseOrder.Status.REFUNDED);
        assertThat(notConfirmed.getStatus()).isEqualTo(PurchaseOrder.Status.COMPLETED);
        wallet(buyer).andExpect(jsonPath("$.balance").value(9600)); // 10000 - 200 - 400 + 200 back
        wallet(seller).andExpect(jsonPath("$.balance").value(10400));
    }

    // An auction with one bid closes: returns the id of the new order
    private Integer sell(User seller, User buyer, String price) {
        Item item = new Item();
        item.setName("Obiect comanda " + price);
        item.setStartPrice(new BigDecimal("100"));
        item.setCurrentPrice(new BigDecimal("100"));
        item.setSeller(seller);
        item.setEndTime(LocalDateTime.now().plusHours(1));
        item = itemRepository.save(item);
        bidService.placeBid(item.getId(), buyer.getUsername(), new BigDecimal(price));
        auctionScheduler.closeAuction(item);
        return orderRepository.findByItem_Id(item.getId()).orElseThrow().getId();
    }

    private ResultActions post(String url, User user, String body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .header("Authorization", bearer(user)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions wallet(User user) throws Exception {
        return mockMvc.perform(get("/api/wallet").header("Authorization", bearer(user)));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
    }

    private User user(String prefix) {
        return user(prefix + "_" + UUID.randomUUID().toString().substring(0, 8), true);
    }

    private User user(String username, boolean exact) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }
}
