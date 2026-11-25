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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Alerte", description = "Cautari salvate: anunt cand apare o licitatie potrivita.")
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    public record AlertRequest(String query, Category category,
                               @PositiveOrZero(message = "Pretul nu poate fi negativ!") BigDecimal minPrice,
                               @PositiveOrZero(message = "Pretul nu poate fi negativ!") BigDecimal maxPrice) {
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Alertele mele")
    @GetMapping
    public List<SavedAlert> list(Authentication authentication) {
        return alertService.list(authentication.getName());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Salveaza o alerta")
    @PostMapping
    public SavedAlert create(@Valid @RequestBody AlertRequest request, Authentication authentication) {
        return alertService.create(authentication.getName(), request.query(), request.category(), request.minPrice(), request.maxPrice());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Sterge o alerta")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        alertService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
