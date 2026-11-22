package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.service.BidService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    // Metoda POST este folosita cand vrem sa cream ceva nou (o licitatie)
    @PostMapping
    public Bid placeBid(@RequestBody BidRequest request) {
        // Preluam datele trimise de utilizator pe web si le trimitem catre "Creier" (BidService)
        return bidService.placeBid(request.getItemId(), request.getBidderId(), request.getAmount());
    }

    // Aceasta clasa simpla (DTO) reprezinta forma sub care asteptam datele de la utilizator
    @Data
    public static class BidRequest {
        private Integer itemId;
        private Integer bidderId;
        private BigDecimal amount;
    }
}