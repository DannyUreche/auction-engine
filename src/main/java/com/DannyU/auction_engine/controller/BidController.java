package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.service.BidService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping
    public Bid placeBid(@RequestBody BidRequest request, Authentication authentication) {
        // authentication.getName() returneaza exact username-ul extras din token-ul JWT
        return bidService.placeBid(request.getItemId(), authentication.getName(), request.getAmount());
    }

    @Data
    public static class BidRequest {
        private Integer itemId;
        private BigDecimal amount;
    }
}