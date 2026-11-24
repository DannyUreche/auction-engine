package com.DannyU.auction_engine.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cifrele si ultimele oferte de pe pagina principala sunt publice si nu contin emailuri
@SpringBootTest
@AutoConfigureMockMvc
class PublicStatsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statisticilePubliceSuntVizibileFaraCont() throws Exception {
        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeItems").isNumber())
                .andExpect(jsonPath("$.bids").isNumber())
                .andExpect(jsonPath("$.soldValue").isNumber());
    }

    @Test
    void ultimeleOfertePublicefaraEmailuri() throws Exception {
        mockMvc.perform(get("/api/bids/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(8)))
                .andExpect(jsonPath("$[*].bidder.email").isEmpty())
                .andExpect(jsonPath("$[*].item.seller.email").isEmpty());
    }
}
