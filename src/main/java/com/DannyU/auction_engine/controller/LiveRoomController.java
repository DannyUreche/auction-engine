package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.LiveComment;
import com.DannyU.auction_engine.service.LiveRoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Live auction room: viewer count (public heartbeat) and the public live chat
@io.swagger.v3.oas.annotations.tags.Tag(name = "Camera live", description = "Prezenta (cati se uita) si chat-ul public de pe pagina obiectului.")
@RestController
@RequestMapping("/api/items/{id}")
@RequiredArgsConstructor
public class LiveRoomController {

    private final LiveRoomService liveRoomService;

    public record PresenceRequest(String session) {
    }

    public record Presence(int viewers) {
    }

    public record CommentRequest(@NotBlank(message = "Mesajul nu poate fi gol!")
                                 @Size(max = 300, message = "Mesajul poate avea maxim 300 de caractere!") String text) {
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Semnal de prezenta (la 15 s) - intoarce numarul de privitori")
    @PostMapping("/presence")
    public Presence heartbeat(@PathVariable Integer id, @RequestBody PresenceRequest request, Authentication authentication) {
        String username = authentication == null || authentication instanceof AnonymousAuthenticationToken
                ? null : authentication.getName();
        return new Presence(liveRoomService.heartbeat(id, request.session(), username));
    }

    // Sent with navigator.sendBeacon when the page closes, so the counter drops right away
    @io.swagger.v3.oas.annotations.Operation(summary = "Am parasit pagina")
    @PostMapping("/presence/leave")
    public ResponseEntity<Void> leave(@PathVariable Integer id, @RequestBody PresenceRequest request) {
        liveRoomService.leave(id, request.session());
        return ResponseEntity.noContent().build();
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Comentariile live (ultimele 50)")
    @GetMapping("/comments")
    public List<LiveComment> comments(@PathVariable Integer id) {
        return liveRoomService.comments(id);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Scrie un comentariu live")
    @PostMapping("/comments")
    public LiveComment comment(@PathVariable Integer id, @Valid @RequestBody CommentRequest request, Authentication authentication) {
        return liveRoomService.comment(id, authentication.getName(), request.text());
    }
}
