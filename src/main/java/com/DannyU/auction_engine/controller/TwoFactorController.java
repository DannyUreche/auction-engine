package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.TwoFactorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Managing two-factor authentication for the logged-in user
@RestController
@RequestMapping("/api/users/me/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    public record CodeRequest(String code) {
    }

    public record DisableRequest(String password, String code) {
    }

    @GetMapping
    public TwoFactorService.Status status(Authentication authentication) {
        return twoFactorService.status(authentication.getName());
    }

    @PostMapping("/setup")
    public TwoFactorService.Setup setup(Authentication authentication) {
        return twoFactorService.start(authentication.getName());
    }

    @PostMapping("/enable")
    public TwoFactorService.Enabled enable(@RequestBody CodeRequest request, Authentication authentication) {
        return twoFactorService.confirm(authentication.getName(), request.code());
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable(@RequestBody DisableRequest request, Authentication authentication) {
        twoFactorService.disable(authentication.getName(), request.password(), request.code());
        return ResponseEntity.noContent().build();
    }
}
