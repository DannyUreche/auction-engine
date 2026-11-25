package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Recenziile pentru un obiect vandut (publice la citire; le scriu doar vanzatorul si castigatorul)
@io.swagger.v3.oas.annotations.tags.Tag(name = "Recenzii", description = "Note intre vanzator si castigator dupa o vanzare.")
@RestController
@RequestMapping("/api/items/{id}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    public record ReviewRequest(
            @NotNull(message = "Nota este obligatorie!")
            @Min(value = 1, message = "Nota trebuie sa fie intre 1 si 5 stele!")
            @Max(value = 5, message = "Nota trebuie sa fie intre 1 si 5 stele!")
            Integer rating,
            @Size(max = 500, message = "Comentariul poate avea maxim 500 de caractere!")
            String comment) {
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Recenziile unui obiect")
    @GetMapping
    public List<ReviewService.ReviewDto> list(@PathVariable Integer id) {
        return reviewService.forItem(id);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Lasa o recenzie")
    @PostMapping
    public ReviewService.ReviewDto create(@PathVariable Integer id, @Valid @RequestBody ReviewRequest request,
                                          Authentication authentication) {
        return reviewService.create(id, authentication.getName(), request.rating(), request.comment());
    }
}
