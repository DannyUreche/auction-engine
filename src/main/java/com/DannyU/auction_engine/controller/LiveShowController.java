package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.service.LiveShowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// Live shows: list and details are public, creating one needs a logged-in seller
@io.swagger.v3.oas.annotations.tags.Tag(name = "Licitatii live", description = "Serii de loturi licitate unul dupa altul, pe scena.")
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class LiveShowController {

    private final LiveShowService liveShowService;

    public record ShowRequest(@Size(max = 100, message = "Titlul poate avea maxim 100 de caractere!") String title,
                              LocalDateTime startTime,
                              @NotNull(message = "Alege cate minute dureaza fiecare obiect!") Integer slotMinutes,
                              List<Integer> itemIds) {
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Licitatiile live (programate, in desfasurare, incheiate)")
    @GetMapping
    public List<LiveShowService.ShowSummary> list() {
        return liveShowService.list();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "O licitatie live cu loturile ei")
    @GetMapping("/{id}")
    public LiveShowService.ShowDetails details(@PathVariable Integer id) {
        return liveShowService.details(id);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Obiectele mele care pot intra intr-o licitatie live")
    @GetMapping("/eligible-items")
    public List<Item> eligible(Authentication authentication) {
        return liveShowService.eligibleItems(authentication.getName());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Organizeaza o licitatie live")
    @PostMapping
    public LiveShowService.ShowDetails create(@Valid @RequestBody ShowRequest request, Authentication authentication) {
        return liveShowService.create(authentication.getName(), request.title(), request.startTime(), request.slotMinutes(), request.itemIds());
    }
}
