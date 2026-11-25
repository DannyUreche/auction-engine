package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Personal lists of auctions. GET /{id} and a user's public lists are open to everyone; the rest is the owner's.
@RestController
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    public record CollectionRequest(String title, String description, Boolean publicList) {
        boolean isPublic() {
            return publicList == null || publicList;
        }
    }

    @GetMapping("/api/collections/mine")
    public List<CollectionService.Summary> mine(Authentication authentication) {
        return collectionService.mine(authentication.getName());
    }

    @GetMapping("/api/collections/containing/{itemId}")
    public List<Integer> containing(@PathVariable Integer itemId, Authentication authentication) {
        return collectionService.containing(itemId, authentication.getName());
    }

    @GetMapping("/api/collections/{id}")
    public CollectionService.Details details(@PathVariable Integer id, Authentication authentication) {
        return collectionService.details(id, authentication == null ? null : authentication.getName());
    }

    @GetMapping("/api/users/{username}/collections")
    public List<CollectionService.Summary> publicOf(@PathVariable String username) {
        return collectionService.publicOf(username);
    }

    @PostMapping("/api/collections")
    public CollectionService.Summary create(@RequestBody CollectionRequest request, Authentication authentication) {
        return collectionService.create(authentication.getName(), request.title(), request.description(), request.isPublic());
    }

    @PutMapping("/api/collections/{id}")
    public CollectionService.Summary update(@PathVariable Integer id, @RequestBody CollectionRequest request, Authentication authentication) {
        return collectionService.update(id, authentication.getName(), request.title(), request.description(), request.isPublic());
    }

    @DeleteMapping("/api/collections/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        collectionService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/collections/{id}/items/{itemId}")
    public ResponseEntity<Void> add(@PathVariable Integer id, @PathVariable Integer itemId, Authentication authentication) {
        collectionService.addItem(id, itemId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/collections/{id}/items/{itemId}")
    public ResponseEntity<Void> remove(@PathVariable Integer id, @PathVariable Integer itemId, Authentication authentication) {
        collectionService.removeItem(id, itemId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
