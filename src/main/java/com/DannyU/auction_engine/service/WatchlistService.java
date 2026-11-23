package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.entity.WatchedItem;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.repository.WatchedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Lista de favorite: obiectele pe care un utilizator le urmareste.
// Notificarile (oferta noua / licitatie inchisa) le afiseaza interfata, din evenimentele live.
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchedItemRepository watchedItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public List<Item> watchedItems(String username) {
        return watchedItemRepository.findItemsWatchedBy(username);
    }

    // Incepe urmarirea; daca obiectul e deja urmarit nu se intampla nimic
    @Transactional
    public void watch(Integer itemId, String username) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        if (item.getSeller().getUsername().equals(username)) {
            throw new IllegalArgumentException("Nu poti urmari propriul obiect!");
        }
        if (watchedItemRepository.findByUserUsernameAndItemId(username, itemId).isPresent()) {
            return;
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu exista!"));

        WatchedItem watched = new WatchedItem();
        watched.setUser(user);
        watched.setItem(item);
        watchedItemRepository.save(watched);
    }

    // Opreste urmarirea; daca obiectul nu era urmarit nu se intampla nimic
    @Transactional
    public void unwatch(Integer itemId, String username) {
        watchedItemRepository.findByUserUsernameAndItemId(username, itemId)
                .ifPresent(watchedItemRepository::delete);
    }
}
