package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.JwtService;
import com.DannyU.auction_engine.service.StripeGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Card top-ups: the paid Stripe session is credited exactly once, only to its owner (Stripe is mocked)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CardTopUpApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @MockitoBean
    private StripeGateway stripe;

    @Test
    void aPaidSessionIsCreditedOnce() throws Exception {
        User ana = user("card_a");
        User ion = user("card_i");
        String session = "cs_test_" + UUID.randomUUID().toString().replace("-", "");
        when(stripe.isEnabled()).thenReturn(true);
        when(stripe.isTestMode()).thenReturn(true);
        when(stripe.createCheckout(eq(ana.getUsername()), anyLong(), anyString(), anyString()))
                .thenReturn(new StripeGateway.Session(session, "https://checkout.stripe.com/c/pay/" + session, false, 25000, "ron", ana.getUsername()));
        when(stripe.retrieve(session)).thenReturn(new StripeGateway.Session(session, null, true, 25000, "ron", ana.getUsername()));

        mockMvc.perform(get("/api/wallet/card").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.enabled").value(true)).andExpect(jsonPath("$.testMode").value(true));
        checkout(ana, 5).andExpect(status().isBadRequest());
        checkout(ana, 250).andExpect(status().isOk()).andExpect(jsonPath("$.url").value("https://checkout.stripe.com/c/pay/" + session));

        confirm(ion, session).andExpect(jsonPath("$.eroare").value("Aceasta plata nu iti apartine."));
        confirm(ana, session).andExpect(jsonPath("$.credited").value(true)).andExpect(jsonPath("$.amount").value(250));
        confirm(ana, session).andExpect(jsonPath("$.credited").value(false));
        mockMvc.perform(get("/api/wallet").header("Authorization", bearer(ana)))
                .andExpect(jsonPath("$.balance").value(10250))
                .andExpect(jsonPath("$.transactions[0].description").value("Alimentare cu cardul (Stripe, mod test)"));

        // The webhook only accepts what the gateway verified
        when(stripe.completedSessionId(anyString(), eq("bad"))).thenThrow(new IllegalArgumentException("Semnatura Stripe invalida."));
        mockMvc.perform(post("/api/stripe/webhook").header("Stripe-Signature", "bad").content("{}"))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions checkout(User user, long amount) throws Exception {
        return mockMvc.perform(post("/api/wallet/checkout").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": " + amount + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions confirm(User user, String session) throws Exception {
        return mockMvc.perform(post("/api/wallet/checkout/confirm").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"sessionId\": \"" + session + "\"}"));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getUsername());
    }

    private User user(String prefix) {
        User user = new User();
        user.setUsername(prefix + "_" + UUID.randomUUID().toString().substring(0, 8));
        user.setEmail(user.getUsername() + "@test.ro");
        user.setPassword("x");
        return userRepository.save(user);
    }
}
