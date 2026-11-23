package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.AuctionEvent;
import com.DannyU.auction_engine.service.AuctionEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Fluxul de evenimente in timp real (Server-Sent Events) de pe /api/events
@SpringBootTest
@AutoConfigureMockMvc
class EventsApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuctionEventService auctionEventService;

    @Test
    void browserulConectatPrimesteEvenimenteleFaraToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        assertThat(result.getResponse().getContentType()).startsWith("text/event-stream");
        assertThat(result.getResponse().getContentAsString()).contains("event:connected");

        auctionEventService.onAuctionEvent(AuctionEvent.bid(12345, new BigDecimal("150.00"), "ion"));
        auctionEventService.onAuctionEvent(AuctionEvent.closed(12345, new BigDecimal("150.00"), "ion"));

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .contains("event:bid")
                .contains("event:closed")
                .contains("\"itemId\":12345")
                .contains("\"username\":\"ion\"");
    }
}
