package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.BidRepository;
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
    private final BidRepository bidRepository;

    // "Cumpara acum": utilizatorul logat plateste pretul fix si castiga imediat
    @PostMapping("/{id}/buy-now")
    public Bid buyNow(@PathVariable Integer id, Authentication authentication) {
        return bidService.buyNow(id, authentication.getName());
    }

    // ---------- Pozele obiectului (maxim Item.MAX_IMAGES) - le poate schimba doar vanzatorul ----------

    // Adauga o poza la sfarsitul listei (prima poza adaugata devine poza principala)
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Item addImage(@PathVariable Integer id,
                         @RequestParam("image") MultipartFile image,
                         Authentication authentication) {
        Item item = findOwnItem(id, authentication);
        if (item.getImageFilenames().size() >= Item.MAX_IMAGES) {
            throw new IllegalArgumentException("Un obiect poate avea maxim " + Item.MAX_IMAGES + " poze!");
        }
        item.getImageFilenames().add(imageStorageService.store(image));
        return itemRepository.save(item);
    }

    // Sterge o poza (filename = ultima parte din imageUrl, ex: 3f2a...c1.jpg)
    @DeleteMapping("/{id}/images/{filename}")
    public Item deleteImage(@PathVariable Integer id, @PathVariable String filename, Authentication authentication) {
        Item item = findOwnItem(id, authentication);
        if (!item.getImageFilenames().remove(filename)) {
            throw new IllegalArgumentException("Poza nu a fost gasita!");
        }
        Item saved = itemRepository.saveAndFlush(item);
        // Imaginea se sterge abia dupa ce obiectul nu o mai foloseste (cheie straina)
        imageStorageService.delete(filename);
        return saved;
    }

    // Muta poza pe primul loc: devine poza principala, afisata pe card
    @PostMapping("/{id}/images/{filename}/main")
    public Item setMainImage(@PathVariable Integer id, @PathVariable String filename, Authentication authentication) {
        Item item = findOwnItem(id, authentication);
        if (!item.getImageFilenames().remove(filename)) {
            throw new IllegalArgumentException("Poza nu a fost gasita!");
        }
        item.getImageFilenames().addFirst(filename);
        return itemRepository.save(item);
    }

    private Item findOwnItem(Integer id, Authentication authentication) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        if (!item.getSeller().getUsername().equals(authentication.getName())) {
            throw new IllegalArgumentException("Doar vanzatorul poate schimba pozele obiectului!");
        }
        return item;
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

    // Editarea obiectului de catre vanzator: doar cat timp licitatia e deschisa si nu exista oferte
    // (dupa prima oferta, cei care liciteaza trebuie sa poata avea incredere in ce au vazut)
    @PutMapping("/{id}")
    public Item updateItem(@PathVariable Integer id, @Valid @RequestBody ItemUpdateRequest request,
                           Authentication authentication) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Obiectul nu a fost gasit!"));
        if (!item.getSeller().getUsername().equals(authentication.getName())) {
            throw new IllegalArgumentException("Doar vanzatorul poate modifica obiectul!");
        }
        if (!item.getActive()) {
            throw new IllegalArgumentException("Licitatia s-a incheiat, obiectul nu mai poate fi modificat!");
        }
        if (bidRepository.countByItemId(id) > 0) {
            throw new IllegalArgumentException("Obiectul are deja oferte si nu mai poate fi modificat!");
        }
        if (request.getBuyNowPrice() != null && request.getBuyNowPrice().compareTo(request.getStartPrice()) <= 0) {
            throw new IllegalArgumentException("Pretul \"Cumpara acum\" trebuie sa fie mai mare decat pretul de pornire!");
        }

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setCategory(request.getCategory() != null ? request.getCategory() : Category.ALTELE);
        item.setStartPrice(request.getStartPrice());
        // Fara oferte, pretul curent este chiar pretul de pornire
        item.setCurrentPrice(request.getStartPrice());
        item.setBuyNowPrice(request.getBuyNowPrice());
        return itemRepository.save(item);
    }

    // Aceleasi campuri ca la creare, fara durata (data de incheiere nu se schimba)
    @Data
    public static class ItemUpdateRequest {

        @NotBlank(message = "Numele obiectului este obligatoriu!")
        @Size(max = 100, message = "Numele obiectului poate avea maxim 100 de caractere!")
        private String name;

        private String description;

        @NotNull(message = "Pretul de pornire este obligatoriu!")
        @Positive(message = "Pretul de pornire trebuie sa fie mai mare decat 0!")
        @Digits(integer = 8, fraction = 2, message = "Pretul poate avea maxim 8 cifre si 2 zecimale!")
        private BigDecimal startPrice;

        private Category category;

        @Positive(message = "Pretul \"Cumpara acum\" trebuie sa fie mai mare decat 0!")
        @Digits(integer = 8, fraction = 2, message = "Pretul poate avea maxim 8 cifre si 2 zecimale!")
        private BigDecimal buyNowPrice;
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