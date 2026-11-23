package com.DannyU.auction_engine.controller;

import com.DannyU.auction_engine.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

// Trimite imaginile obiectelor catre browser. Ruta este publica (tag-ul <img> nu trimite token-ul JWT).
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageStorageService imageStorageService;

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        return imageStorageService.find(filename)
                .map(path -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(imageStorageService.contentType(filename)))
                        // Numele fisierului se schimba la fiecare imagine noua, deci browserul o poate tine in cache
                        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                        .<Resource>body(new FileSystemResource(path)))
                .orElse(ResponseEntity.notFound().build());
    }
}
