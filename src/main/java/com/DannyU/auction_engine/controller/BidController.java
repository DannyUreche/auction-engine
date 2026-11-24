package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.repository.BidRepository;
import com.DannyU.auction_engine.service.BidService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    private final BidRepository bidRepository;

    @PostMapping
    public Bid placeBid(@Valid @RequestBody BidRequest request, Authentication authentication) {
        // authentication.getName() returneaza exact username-ul extras din token-ul JWT
        return bidService.placeBid(request.getItemId(), authentication.getName(), request.getAmount());
    }

    @Data
    public static class BidRequest {
        @NotNull(message = "Id-ul obiectului este obligatoriu!")
        private Integer itemId;

        @NotNull(message = "Suma oferita este obligatorie!")
        @Positive(message = "Suma oferita trebuie sa fie mai mare decat 0!")
        @Digits(integer = 8, fraction = 2, message = "Suma poate avea maxim 8 cifre si 2 zecimale!")
        private BigDecimal amount;
    }

    // Ofertele utilizatorului logat
    // Ultimele 8 oferte de pe tot site-ul (publice, ca istoricul ofertelor)
    @GetMapping("/recent")
    public List<Bid> getRecentBids() {
        return bidRepository.findTop8ByOrderByCreatedAtDesc();
    }

    @GetMapping("/my")
    public List<Bid> getMyBids(Authentication authentication) {
        return bidService.getBidsForUser(authentication.getName());
    }

    @GetMapping("/{itemId}")
    public List<Bid> getItemBids(@PathVariable Integer itemId) {
        return bidService.getBidsForItem(itemId);
    }
}