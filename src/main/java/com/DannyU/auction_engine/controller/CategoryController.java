package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Category;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

// Lista categoriilor, folosita de interfata (filtre si formularul "Vinde un obiect"). Publica.
@io.swagger.v3.oas.annotations.tags.Tag(name = "Categorii", description = "Lista fixa de categorii.")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @io.swagger.v3.oas.annotations.Operation(summary = "Toate categoriile")
    @GetMapping
    public List<CategoryDto> categories() {
        return Arrays.stream(Category.values())
                .map(c -> new CategoryDto(c.name(), c.getLabel(), c.getIcon()))
                .toList();
    }

    public record CategoryDto(String code, String label, String icon) {
    }
}
