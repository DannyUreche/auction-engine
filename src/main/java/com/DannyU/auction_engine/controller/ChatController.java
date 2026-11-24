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
}
