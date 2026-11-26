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
    private final com.DannyU.auction_engine.service.LiveStreamService liveStreamService;

    public record PeerRequest(String peer) {
    }

    public record SignalRequest(String from, String to, String kind, String data) {
    }

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

    // ---------- Video (WebRTC signaling, see LiveStreamService) ----------

    @io.swagger.v3.oas.annotations.Operation(summary = "Transmite vanzatorul video acum? Cati privitori are?")
    @GetMapping("/{id}/stream")
    public com.DannyU.auction_engine.service.LiveStreamService.Status streamStatus(@PathVariable Integer id) {
        return liveStreamService.status(id);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Organizatorul porneste camera")
    @PostMapping("/{id}/stream/start")
    public com.DannyU.auction_engine.service.LiveStreamService.Status startStream(@PathVariable Integer id, Authentication authentication) {
        return liveStreamService.start(id, authentication.getName());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Organizatorul opreste camera")
    @PostMapping("/{id}/stream/stop")
    public org.springframework.http.ResponseEntity<Void> stopStream(@PathVariable Integer id, Authentication authentication) {
        liveStreamService.stop(id, authentication.getName());
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Un privitor cere video-ul (peer = id aleator generat de browser)")
    @PostMapping("/{id}/stream/join")
    public com.DannyU.auction_engine.service.LiveStreamService.Status joinStream(@PathVariable Integer id, @RequestBody PeerRequest request) {
        return liveStreamService.join(id, request.peer());
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Un privitor iese din transmisiune")
    @PostMapping("/{id}/stream/leave")
    public org.springframework.http.ResponseEntity<Void> leaveStream(@PathVariable Integer id, @RequestBody PeerRequest request) {
        liveStreamService.leave(id, request.peer());
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Trimite un mesaj de conectare WebRTC (offer, answer sau ice)")
    @PostMapping("/{id}/stream/signal")
    public org.springframework.http.ResponseEntity<Void> signal(@PathVariable Integer id, @RequestBody SignalRequest request, Authentication authentication) {
        liveStreamService.signal(id, request.from(), request.to(), request.kind(), request.data(), authentication == null ? null : authentication.getName());
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Ia mesajele de conectare care asteapta (inbox-ul gazdei doar pentru organizator)")
    @GetMapping("/{id}/stream/inbox")
    public List<com.DannyU.auction_engine.service.LiveStreamService.Signal> inbox(@PathVariable Integer id, @RequestParam String peer, Authentication authentication) {
        return liveStreamService.inbox(id, peer, authentication == null ? null : authentication.getName());
    }
}
