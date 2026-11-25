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
}
