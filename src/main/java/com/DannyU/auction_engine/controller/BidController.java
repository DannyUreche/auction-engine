package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.service.BidService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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
        private Long itemId; // Aici am modificat din Integer in Long
        private BigDecimal amount;
    }

    // Ofertele utilizatorului logat
    @GetMapping("/my")
    public List<Bid> getMyBids(Authentication authentication) {
        return bidService.getBidsForUser(authentication.getName());
    }

    @GetMapping("/{itemId}")
    public List<Bid> getItemBids(@PathVariable Long itemId) {
        return bidService.getBidsForItem(itemId);
    }
}