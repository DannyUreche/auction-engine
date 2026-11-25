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

    @GetMapping
    public List<LiveShowService.ShowSummary> list() {
        return liveShowService.list();
    }

    @GetMapping("/{id}")
    public LiveShowService.ShowDetails details(@PathVariable Integer id) {
        return liveShowService.details(id);
    }

    @GetMapping("/eligible-items")
    public List<Item> eligible(Authentication authentication) {
        return liveShowService.eligibleItems(authentication.getName());
    }

    @PostMapping
    public LiveShowService.ShowDetails create(@Valid @RequestBody ShowRequest request, Authentication authentication) {
        return liveShowService.create(authentication.getName(), request.title(), request.startTime(), request.slotMinutes(), request.itemIds());
    }
}
