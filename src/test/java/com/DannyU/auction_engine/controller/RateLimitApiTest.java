package com.DannyU.auction_engine.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Limita de incercari la login (aici 3 pe minut, ca testul sa fie scurt)
@SpringBootTest(properties = "app.rate-limit.auth-per-minute=3")
@AutoConfigureMockMvc
class RateLimitApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dupaPreaMulteIncercariLoginulEsteBlocatTemporar() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("1.2.3.4").andExpect(status().isBadRequest());
        }
        login("1.2.3.4")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.eroare").value(startsWith("Prea multe incercari!")));

        // Alt IP nu este afectat
        login("5.6.7.8").andExpect(status().isBadRequest());
    }

    private ResultActions login(String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"nu_exista_rate\", \"password\": \"gresita\"}"));
    }
}
