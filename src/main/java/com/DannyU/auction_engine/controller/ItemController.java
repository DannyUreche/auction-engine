package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository; // Avem nevoie de el ca sa gasim vanzatorul

    @GetMapping
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    // Endpoint nou pentru adaugarea unui obiect de vanzare
    @PostMapping
    public Item createItem(@RequestBody ItemRequest request) {

        // 1. Gasim vanzatorul in baza de date
        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new IllegalArgumentException("Vanzatorul nu exista!"));

        // 2. Cream obiectul nou
        Item newItem = new Item();
        newItem.setName(request.getName());
        newItem.setDescription(request.getDescription());
        newItem.setStartPrice(request.getStartPrice());
        newItem.setCurrentPrice(request.getStartPrice()); // La inceput, pretul curent e acelasi cu cel de pornire
        newItem.setSeller(seller); // Legam obiectul de vanzator

        // 3. Il salvam in baza de date
        return itemRepository.save(newItem);
    }

    // Forma sub care asteptam datele pentru crearea unui Item
    @Data
    public static class ItemRequest {
        private String name;
        private String description;
        private BigDecimal startPrice;
        private Integer sellerId;
    }
}