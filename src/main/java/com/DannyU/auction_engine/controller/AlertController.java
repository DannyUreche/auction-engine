package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.entity.SavedAlert;
import com.DannyU.auction_engine.service.AlertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// The logged-in user's saved searches ("alerts")
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    public record AlertRequest(String query, Category category,
                               @PositiveOrZero(message = "Pretul nu poate fi negativ!") BigDecimal minPrice,
                               @PositiveOrZero(message = "Pretul nu poate fi negativ!") BigDecimal maxPrice) {
    }

    @GetMapping
    public List<SavedAlert> list(Authentication authentication) {
        return alertService.list(authentication.getName());
    }

    @PostMapping
    public SavedAlert create(@Valid @RequestBody AlertRequest request, Authentication authentication) {
        return alertService.create(authentication.getName(), request.query(), request.category(), request.minPrice(), request.maxPrice());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        alertService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
