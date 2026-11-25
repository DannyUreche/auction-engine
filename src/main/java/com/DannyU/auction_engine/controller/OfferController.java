package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Offer;
import com.DannyU.auction_engine.service.OfferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// Private offers on an item (all routes need a logged-in user)
@RestController
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    public record OfferRequest(@NotNull(message = "Suma este obligatorie!")
                               @Positive(message = "Suma trebuie sa fie mai mare decat 0!")
                               @Digits(integer = 8, fraction = 2, message = "Suma poate avea maxim 8 cifre si 2 zecimale!") BigDecimal amount,
                               @Size(max = 300, message = "Mesajul poate avea maxim 300 de caractere!") String message) {
    }

    public record ReplyRequest(@NotNull(message = "Alege un raspuns!") OfferService.Action action, BigDecimal counterAmount) {
    }

    @GetMapping("/api/items/{id}/offers")
    public List<Offer> offers(@PathVariable Integer id, Authentication authentication) {
        return offerService.offersFor(id, authentication.getName());
    }

    @PostMapping("/api/items/{id}/offers")
    public Offer make(@PathVariable Integer id, @Valid @RequestBody OfferRequest request, Authentication authentication) {
        return offerService.make(id, authentication.getName(), request.amount(), request.message());
    }

    @PostMapping("/api/offers/{offerId}/reply")
    public Offer reply(@PathVariable Integer offerId, @Valid @RequestBody ReplyRequest request, Authentication authentication) {
        return offerService.respond(offerId, authentication.getName(), request.action(), request.counterAmount());
    }
}
