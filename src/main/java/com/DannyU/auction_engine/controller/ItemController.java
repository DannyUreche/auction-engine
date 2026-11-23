package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @GetMapping
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    // Doar licitatiile inca deschise
    @GetMapping("/active")
    public List<Item> getActiveItems() {
        return itemRepository.findByActiveTrueOrderByEndTimeAsc();
    }

    // Obiectele puse la licitatie de utilizatorul logat
    @GetMapping("/my")
    public List<Item> getMyItems(Authentication authentication) {
        return itemRepository.findBySellerUsernameOrderByIdDesc(authentication.getName());
    }

    // Obiectele castigate de utilizatorul logat
    @GetMapping("/won")
    public List<Item> getWonItems(Authentication authentication) {
        return itemRepository.findByWinnerUsernameOrderByEndTimeDesc(authentication.getName());
    }

    // Detaliile unui singur obiect
    @GetMapping("/{id}")
    public Item getItem(@PathVariable Integer id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
    }

    @PostMapping
    public Item createItem(@Valid @RequestBody ItemRequest request, Authentication authentication) {
        // Luam utilizatorul logat direct din JWT
        User seller = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Vanzatorul nu exista!"));

        Item newItem = new Item();
        newItem.setName(request.getName());
        newItem.setDescription(request.getDescription());
        newItem.setStartPrice(request.getStartPrice());
        newItem.setCurrentPrice(request.getStartPrice());
        newItem.setSeller(seller);

        // Durata este optionala: implicit 60 de minute
        int minutes = request.getDurationInMinutes() != null ? request.getDurationInMinutes() : 60;
        newItem.setEndTime(LocalDateTime.now().plusMinutes(minutes));

        return itemRepository.save(newItem);
    }

    @Data
    public static class ItemRequest {

        @NotBlank(message = "Numele obiectului este obligatoriu!")
        @Size(max = 100, message = "Numele obiectului poate avea maxim 100 de caractere!")
        private String name;

        private String description;

        @NotNull(message = "Pretul de pornire este obligatoriu!")
        @Positive(message = "Pretul de pornire trebuie sa fie mai mare decat 0!")
        @Digits(integer = 8, fraction = 2, message = "Pretul poate avea maxim 8 cifre si 2 zecimale!")
        private BigDecimal startPrice;

        @Positive(message = "Durata licitatiei trebuie sa fie de cel putin un minut!")
        private Integer durationInMinutes;
    }
}