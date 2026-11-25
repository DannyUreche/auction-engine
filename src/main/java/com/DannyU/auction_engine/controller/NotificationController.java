package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Notificarile utilizatorului logat (clopotelul din bara de sus)
@io.swagger.v3.oas.annotations.tags.Tag(name = "Notificari", description = "Notificarile utilizatorului curent.")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Notificarile mele")
    @GetMapping
    public NotificationService.NotificationList list(Authentication authentication) {
        return notificationService.list(authentication.getName());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Marcheaza toate ca citite")
    @PostMapping("/read-all")
    public NotificationService.NotificationList markAllRead(Authentication authentication) {
        notificationService.markAllRead(authentication.getName());
        return notificationService.list(authentication.getName());
    }
}
