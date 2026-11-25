package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.TwoFactorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Managing two-factor authentication for the logged-in user
@io.swagger.v3.oas.annotations.tags.Tag(name = "Autentificare in doi pasi", description = "Activarea si dezactivarea codurilor din aplicatia de autentificare (TOTP).")
@RestController
@RequestMapping("/api/users/me/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    public record CodeRequest(String code) {
    }

    public record DisableRequest(String password, String code) {
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Starea autentificarii in doi pasi")
    @GetMapping
    public TwoFactorService.Status status(Authentication authentication) {
        return twoFactorService.status(authentication.getName());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Incepe activarea (secret + link otpauth pentru QR)")
    @PostMapping("/setup")
    public TwoFactorService.Setup setup(Authentication authentication) {
        return twoFactorService.start(authentication.getName());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Confirma cu un cod si activeaza (primesti codurile de rezerva)")
    @PostMapping("/enable")
    public TwoFactorService.Enabled enable(@RequestBody CodeRequest request, Authentication authentication) {
        return twoFactorService.confirm(authentication.getName(), request.code());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Dezactiveaza (cu parola si un cod)")
    @PostMapping("/disable")
    public ResponseEntity<Void> disable(@RequestBody DisableRequest request, Authentication authentication) {
        twoFactorService.disable(authentication.getName(), request.password(), request.code());
        return ResponseEntity.noContent().build();
    }
}
