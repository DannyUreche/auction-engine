package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.PushNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Push notifications on this device (the browser sends what PushManager.subscribe() returned)
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;

    public record PublicKey(String publicKey) {
    }

    public record Keys(@NotBlank String p256dh, @NotBlank String auth) {
    }

    public record SubscriptionRequest(@NotBlank(message = "Abonament invalid!") String endpoint,
                                      @NotNull(message = "Abonament invalid!") @Valid Keys keys) {
    }

    public record EndpointRequest(@NotBlank(message = "Abonament invalid!") String endpoint) {
    }

    // Public: the browser needs it before subscribing
    @GetMapping("/public-key")
    public PublicKey publicKey() {
        return new PublicKey(pushNotificationService.publicKey());
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody SubscriptionRequest request, Authentication authentication) {
        pushNotificationService.subscribe(userRepository.findByUsername(authentication.getName()).orElseThrow(),
                request.endpoint(), request.keys().p256dh(), request.keys().auth());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody EndpointRequest request) {
        pushNotificationService.unsubscribe(request.endpoint());
        return ResponseEntity.noContent().build();
    }
}
