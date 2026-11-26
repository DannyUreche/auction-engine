package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

// The virtual wallet of the logged-in user
@io.swagger.v3.oas.annotations.tags.Tag(name = "Portofel", description = "Sold virtual (credite de test) si tranzactii.")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final com.DannyU.auction_engine.service.CardTopUpService cardTopUpService;

    public record CheckoutRequest(Long amount) {
    }

    public record ConfirmRequest(String sessionId) {
    }

    public record TopUpRequest(@NotNull(message = "Suma este obligatorie!") BigDecimal amount) {
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Portofelul meu (sold, rezervat, disponibil, tranzactii)")
    @GetMapping
    public WalletService.Wallet wallet(Authentication authentication) {
        return walletService.wallet(authentication.getName());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Adauga credite de test (maxim 5000 odata)")
    @PostMapping("/top-up")
    public WalletService.Wallet topUp(@Valid @RequestBody TopUpRequest request, Authentication authentication) {
        return walletService.topUp(authentication.getName(), request.amount());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Este activa plata cu cardul (Stripe)?")
    @GetMapping("/card")
    public com.DannyU.auction_engine.service.CardTopUpService.CardConfig cardConfig() {
        return cardTopUpService.config();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Incepe o plata cu cardul: intoarce adresa paginii Stripe Checkout")
    @PostMapping("/checkout")
    public com.DannyU.auction_engine.service.CardTopUpService.Checkout checkout(@RequestBody CheckoutRequest request, Authentication authentication) {
        return cardTopUpService.start(authentication.getName(), request.amount());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Confirma plata dupa intoarcerea de la Stripe (o singura alimentare per plata)")
    @PostMapping("/checkout/confirm")
    public com.DannyU.auction_engine.service.CardTopUpService.Result confirm(@RequestBody ConfirmRequest request, Authentication authentication) {
        return cardTopUpService.confirm(authentication.getName(), request.sessionId());
    }
}
