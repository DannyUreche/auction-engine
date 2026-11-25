package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.entity.Message;
import com.DannyU.auction_engine.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Chat-ul unui obiect vandut (vanzator <-> castigator)
@RestController
@RequestMapping("/api/items/{id}/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    public record MessageRequest(
            @NotBlank(message = "Mesajul nu poate fi gol!")
            @Size(max = 1000, message = "Mesajul poate avea maxim 1000 de caractere!")
            String text) {
    }

    @GetMapping
    public List<Message> list(@PathVariable Integer id, Authentication authentication) {
        return chatService.messages(id, authentication.getName());
    }

    @PostMapping
    public Message send(@PathVariable Integer id, @Valid @RequestBody MessageRequest request, Authentication authentication) {
        return chatService.send(id, authentication.getName(), request.text());
    }

    // The chat is open on screen: the other participant's messages count as read
    @PostMapping("/read")
    public java.util.Map<String, Integer> read(@PathVariable Integer id, Authentication authentication) {
        return java.util.Map.of("read", chatService.markRead(id, authentication.getName()));
    }

    // Typing indicator for the other participant (nothing stored)
    @PostMapping("/typing")
    public org.springframework.http.ResponseEntity<Void> typing(@PathVariable Integer id, Authentication authentication) {
        chatService.typing(id, authentication.getName());
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}
