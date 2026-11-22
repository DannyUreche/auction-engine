package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController // Indica faptul ca aceasta clasa expune URL-uri (REST API)
@RequestMapping("/api/items") // Toate linkurile din aceasta clasa vor incepe cu /api/items
@RequiredArgsConstructor
public class ItemController {

    private final ItemRepository itemRepository;

    // Cand cineva acceseaza URL-ul de mai sus cu metoda GET (ex: din browser)
    @GetMapping
    public List<Item> getAllItems() {
        // Returneaza toate obiectele din baza de date ca o lista
        return itemRepository.findAll();
    }
}