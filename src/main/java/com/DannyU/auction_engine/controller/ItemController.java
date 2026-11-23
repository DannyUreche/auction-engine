package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.entity.Bid;
import com.DannyU.auction_engine.entity.Category;
import com.DannyU.auction_engine.service.BidService;
import com.DannyU.auction_engine.service.ImageStorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final BidService bidService;

    // "Cumpara acum": utilizatorul logat plateste pretul fix si castiga imediat
    @PostMapping("/{id}/buy-now")
    public Bid buyNow(@PathVariable Integer id, Authentication authentication) {
        return bidService.buyNow(id, authentication.getName());
    }

    // Incarcarea (sau inlocuirea) imaginii unui obiect - doar de catre vanzator
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Item uploadImage(@PathVariable Integer id,
                            @RequestParam("image") MultipartFile image,
                            Authentication authentication) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));

        if (!item.getSeller().getUsername().equals(authentication.getName())) {
            throw new IllegalArgumentException("Doar vanzatorul poate schimba imaginea obiectului!");
        }

        String oldImage = item.getImageFilename();
        item.setImageFilename(imageStorageService.store(image));
        Item saved = itemRepository.save(item);

        // Stergem imaginea veche abia dupa ce noua a fost salvata
        if (oldImage != null) {
            imageStorageService.delete(oldImage);
        }
        return saved;
    }

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

        // Pretul "Cumpara acum" (optional) trebuie sa fie peste pretul de pornire
        if (request.getBuyNowPrice() != null && request.getBuyNowPrice().compareTo(request.getStartPrice()) <= 0) {
            throw new IllegalArgumentException("Pretul \"Cumpara acum\" trebuie sa fie mai mare decat pretul de pornire!");
        }
        newItem.setBuyNowPrice(request.getBuyNowPrice());

        // Categoria e optionala: implicit "Altele"
        newItem.setCategory(request.getCategory() != null ? request.getCategory() : Category.ALTELE);

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

        // Una dintre valorile din Category (ex: "ELECTRONICE"); lipsa = "ALTELE"
        private Category category;

        // Optional: cine plateste acest pret castiga imediat
        @Positive(message = "Pretul \"Cumpara acum\" trebuie sa fie mai mare decat 0!")
        @Digits(integer = 8, fraction = 2, message = "Pretul poate avea maxim 8 cifre si 2 zecimale!")
        private BigDecimal buyNowPrice;
    }
}