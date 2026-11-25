package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.ItemImage;
import com.DannyU.auction_engine.repository.ItemImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;

// Salveaza imaginile obiectelor in baza de date si le citeste inapoi
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final ItemImageRepository itemImageRepository;

    // Tipurile de imagini acceptate, recunoscute dupa primii octeti din fisier (nu dupa extensie)
    private enum ImageType {
        JPG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp"),
        GIF("gif", "image/gif");

        final String extension;
        final String contentType;

        ImageType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }
    }

    // MIME type of an image recognized from its first bytes (empty if it isn't a supported image)
    public Optional<String> contentTypeOf(byte[] data) {
        return detectType(data).map(type -> type.contentType);
    }

    // Salveaza imaginea si intoarce numele generat (UUID + extensie)
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Imaginea este obligatorie!");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Imaginea nu a putut fi citita", e);
        }

        ImageType type = detectType(data)
                .orElseThrow(() -> new IllegalArgumentException("Fisierul trebuie sa fie o imagine JPG, PNG, WebP sau GIF!"));

        ItemImage image = new ItemImage();
        image.setFilename(UUID.randomUUID() + "." + type.extension);
        image.setContentType(type.contentType);
        image.setData(data);
        return itemImageRepository.save(image).getFilename();
    }

    public Optional<ItemImage> find(String filename) {
        return filename == null ? Optional.empty() : itemImageRepository.findById(filename);
    }

    public void delete(String filename) {
        if (filename != null) {
            itemImageRepository.deleteById(filename);
        }
    }

    private Optional<ImageType> detectType(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return Optional.of(ImageType.JPG);
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return Optional.of(ImageType.PNG);
        }
        if (h.length >= 6 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') {
            return Optional.of(ImageType.GIF);
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return Optional.of(ImageType.WEBP);
        }
        return Optional.empty();
    }
}
