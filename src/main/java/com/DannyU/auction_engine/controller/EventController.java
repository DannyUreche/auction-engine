package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.AuctionEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// Flux de evenimente in timp real (oferte noi, licitatii inchise). Public, ca si licitatiile.
// EventSource din browser nu poate trimite token-ul JWT, iar datele trimise sunt oricum publice.
@io.swagger.v3.oas.annotations.tags.Tag(name = "Evenimente live", description = "Flux Server-Sent Events cu ofertele, inchiderile si celelalte schimbari in timp real.")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final AuctionEventService auctionEventService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Flux de evenimente live (text/event-stream)")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return auctionEventService.subscribe();
    }
}
