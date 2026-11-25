package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

// Trimite imaginile obiectelor catre browser. Ruta este publica (tag-ul <img> nu trimite token-ul JWT).
@io.swagger.v3.oas.annotations.tags.Tag(name = "Imagini", description = "Pozele obiectelor (publice, pentru <img>); ?size=thumb da miniatura de 480 px.")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageStorageService imageStorageService;
    private final com.DannyU.auction_engine.service.ThumbnailService thumbnailService;

    // ?size=thumb: a small JPEG for cards and lists (the original when it can't be resized)
    @io.swagger.v3.oas.annotations.Operation(summary = "Miniatura unei poze (480 px, JPEG)")
    @GetMapping(value = "/{filename}", params = "size=thumb")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String filename) {
        return thumbnailService.thumbnail(filename)
                .map(data -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                        .body(data))
                .orElseGet(() -> getImage(filename));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "O poza a unui obiect")
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        return imageStorageService.find(filename)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.getContentType()))
                        // Numele se schimba la fiecare imagine noua, deci browserul o poate tine in cache
                        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                        .body(image.getData()))
                .orElse(ResponseEntity.notFound().build());
    }
}
