package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Notificarile utilizatorului logat (clopotelul din bara de sus)
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationService.NotificationList list(Authentication authentication) {
        return notificationService.list(authentication.getName());
    }

    @PostMapping("/read-all")
    public NotificationService.NotificationList markAllRead(Authentication authentication) {
        notificationService.markAllRead(authentication.getName());
        return notificationService.list(authentication.getName());
    }
}
