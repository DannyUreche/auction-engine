package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.CollectionItem;
import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.ItemCollection;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.CollectionItemRepository;
import com.DannyU.auction_engine.repository.ItemCollectionRepository;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

// Personal lists of auctions ("colectii"): e.g. "Cadouri de Craciun", "Ceasuri de urmarit". Public lists can be
// opened by anyone with the link (and appear on the owner's profile); private ones only by their owner.
@Service
@RequiredArgsConstructor
public class CollectionService {

    private final ItemCollectionRepository collectionRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    // A list card: up to 4 photos for the mosaic
    public record Summary(Integer id, String title, String description, boolean publicList, String owner, long items,
                          List<String> previews, LocalDateTime updatedAt) {
    }

    public record Details(Integer id, String title, String description, boolean publicList, String owner, boolean mine,
                          long activeItems, BigDecimal totalValue, List<Item> items, LocalDateTime updatedAt) {
    }

    @Transactional(readOnly = true)
    public List<Summary> mine(String username) {
        return collectionRepository.findByOwner_IdOrderByUpdatedAtDesc(find(username).getId()).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<Summary> publicOf(String username) {
        return collectionRepository.findByOwner_IdAndPublicListTrueOrderByUpdatedAtDesc(find(username).getId()).stream()
                .map(this::summary).toList();
    }

    // viewer may be null (visitor without an account)
    @Transactional(readOnly = true)
    public Details details(Integer id, String viewer) {
        ItemCollection collection = collectionRepository.findById(id).orElseThrow(CollectionService::notFound);
        boolean mine = viewer != null && viewer.equals(collection.getOwner().getUsername());
        if (!collection.isPublicList() && !mine) {
            throw notFound(); // a private list looks the same as a missing one
        }
        List<Item> items = collectionItemRepository.findByCollection_IdOrderByAddedAtDesc(id).stream().map(CollectionItem::getItem).toList();
        long active = items.stream().filter(item -> Boolean.TRUE.equals(item.getActive())).count();
        BigDecimal total = items.stream().map(Item::getCurrentPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Details(collection.getId(), collection.getTitle(), collection.getDescription(), collection.isPublicList(),
                collection.getOwner().getUsername(), mine, active, total, items, collection.getUpdatedAt());
    }

    @Transactional
    public Summary create(String username, String title, String description, boolean publicList) {
        User owner = find(username);
        if (collectionRepository.countByOwner_Id(owner.getId()) >= ItemCollection.MAX_PER_USER) {
            throw new IllegalArgumentException("Poti avea maxim " + ItemCollection.MAX_PER_USER + " colectii.");
        }
        ItemCollection collection = new ItemCollection();
        collection.setOwner(owner);
        apply(collection, title, description, publicList);
        return summary(collectionRepository.save(collection));
    }

    @Transactional
    public Summary update(Integer id, String username, String title, String description, boolean publicList) {
        ItemCollection collection = own(id, username);
        apply(collection, title, description, publicList);
        return summary(collectionRepository.save(collection));
    }

    @Transactional
    public void delete(Integer id, String username) {
        ItemCollection collection = own(id, username);
        collectionItemRepository.deleteAll(collectionItemRepository.findByCollection_IdOrderByAddedAtDesc(id));
        collectionRepository.delete(collection);
    }

    // Idempotent: adding an item that is already in the list changes nothing
    @Transactional
    public void addItem(Integer id, Integer itemId, String username) {
        ItemCollection collection = own(id, username);
        if (collectionItemRepository.findByCollection_IdAndItem_Id(id, itemId).isPresent()) {
            return;
        }
        if (collectionItemRepository.countByCollection_Id(id) >= ItemCollection.MAX_ITEMS) {
            throw new IllegalArgumentException("O colectie poate avea maxim " + ItemCollection.MAX_ITEMS + " obiecte.");
        }
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        CollectionItem entry = new CollectionItem();
        entry.setCollection(collection);
        entry.setItem(item);
        collectionItemRepository.save(entry);
        touch(collection);
    }

    @Transactional
    public void removeItem(Integer id, Integer itemId, String username) {
        ItemCollection collection = own(id, username);
        collectionItemRepository.findByCollection_IdAndItem_Id(id, itemId).ifPresent(entry -> {
            collectionItemRepository.delete(entry);
            touch(collection);
        });
    }

    // Which of my lists already hold this item (for the "Salveaza in colectie" picker)
    @Transactional(readOnly = true)
    public List<Integer> containing(Integer itemId, String username) {
        return collectionItemRepository.collectionIdsWithItem(find(username).getId(), itemId);
    }

    private void apply(ItemCollection collection, String title, String description, boolean publicList) {
        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException("Da un nume colectiei!");
        }
        if (cleanTitle.length() > 80) {
            throw new IllegalArgumentException("Numele colectiei poate avea maxim 80 de caractere.");
        }
        String cleanDescription = description == null || description.isBlank() ? null : description.trim();
        if (cleanDescription != null && cleanDescription.length() > 500) {
            throw new IllegalArgumentException("Descrierea poate avea maxim 500 de caractere.");
        }
        collection.setTitle(cleanTitle);
        collection.setDescription(cleanDescription);
        collection.setPublicList(publicList);
        collection.setUpdatedAt(LocalDateTime.now());
    }

    private void touch(ItemCollection collection) {
        collection.setUpdatedAt(LocalDateTime.now());
        collectionRepository.save(collection);
    }

    private Summary summary(ItemCollection collection) {
        List<CollectionItem> entries = collectionItemRepository.findByCollection_IdOrderByAddedAtDesc(collection.getId());
        List<String> previews = entries.stream().map(entry -> entry.getItem().getImageUrl()).filter(Objects::nonNull).limit(4).toList();
        return new Summary(collection.getId(), collection.getTitle(), collection.getDescription(), collection.isPublicList(),
                collection.getOwner().getUsername(), entries.size(), previews, collection.getUpdatedAt());
    }

    private ItemCollection own(Integer id, String username) {
        ItemCollection collection = collectionRepository.findById(id).orElseThrow(CollectionService::notFound);
        if (!collection.getOwner().getUsername().equals(username)) {
            throw new IllegalArgumentException("Poti modifica doar colectiile tale!");
        }
        return collection;
    }

    private static IllegalArgumentException notFound() {
        return new IllegalArgumentException("Colectia nu a fost gasita!");
    }

    private User find(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu a fost gasit!"));
    }
}
