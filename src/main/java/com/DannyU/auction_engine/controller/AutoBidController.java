package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.BidService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

// Licitarea automata a utilizatorului logat pentru un obiect (suma maxima o vede doar el)
@io.swagger.v3.oas.annotations.tags.Tag(name = "Licitare automata", description = "Suma maxima pana la care aplicatia liciteaza in locul tau (proxy bidding).")
@RestController
@RequestMapping("/api/items/{id}/auto-bid")
@RequiredArgsConstructor
public class AutoBidController {

    private final BidService bidService;

    public record AutoBidRequest(
            @NotNull(message = "Suma maxima este obligatorie!")
            @Positive(message = "Suma maxima trebuie sa fie mai mare decat 0!")
            @Digits(integer = 8, fraction = 2, message = "Suma poate avea maxim 8 cifre si 2 zecimale!")
            BigDecimal maxAmount) {
    }

    // maxAmount = null cand utilizatorul nu are licitare automata la acest obiect
    public record AutoBidStatus(Integer itemId, BigDecimal maxAmount) {
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Licitarea mea automata pe acest obiect")
    @GetMapping
    public AutoBidStatus get(@PathVariable Integer id, Authentication authentication) {
        return new AutoBidStatus(id, bidService.getAutoBid(id, authentication.getName())
                .map(a -> a.getMaxAmount()).orElse(null));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Seteaza sau schimba suma maxima")
    @PutMapping
    public AutoBidStatus set(@PathVariable Integer id, @Valid @RequestBody AutoBidRequest request,
                             Authentication authentication) {
        return new AutoBidStatus(id, bidService.setAutoBid(id, authentication.getName(), request.maxAmount()).getMaxAmount());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Opreste licitarea automata")
    @DeleteMapping
    public AutoBidStatus cancel(@PathVariable Integer id, Authentication authentication) {
        bidService.cancelAutoBid(id, authentication.getName());
        return new AutoBidStatus(id, null);
    }
}
