package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
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
    public Item getItem(@PathVariable Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
    }

    @PostMapping
    public Item createItem(@RequestBody ItemRequest request, Authentication authentication) {
        // Luam utilizatorul logat direct din JWT
        User seller = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Vanzatorul nu exista!"));

        Item newItem = new Item();
        newItem.setName(request.getName());
        newItem.setDescription(request.getDescription());
        newItem.setStartPrice(request.getStartPrice());
        newItem.setCurrentPrice(request.getStartPrice());
        newItem.setSeller(seller);

        int minutes = (request.getDurationInMinutes() != null && request.getDurationInMinutes() > 0)
                ? request.getDurationInMinutes() : 60;
        newItem.setEndTime(LocalDateTime.now().plusMinutes(minutes));

        return itemRepository.save(newItem);
    }

    @Data
    public static class ItemRequest {
        private String name;
        private String description;
        private BigDecimal startPrice;
        private Integer durationInMinutes;
    }
}