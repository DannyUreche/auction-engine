package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.CardTopUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// Stripe calls this after a card payment (public, but only signed requests are accepted)
@Tag(name = "Plati", description = "Notificarile Stripe dupa plata cu cardul (doar cereri semnate de Stripe).")
@RestController
@RequiredArgsConstructor
public class StripeWebhookController {

    private final CardTopUpService cardTopUpService;

    @Operation(summary = "Webhook Stripe: checkout.session.completed alimenteaza portofelul")
    @PostMapping("/api/stripe/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        cardTopUpService.webhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
