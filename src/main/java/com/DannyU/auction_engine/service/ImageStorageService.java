package com.DannyU.auction_engine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

// Salveaza imaginile obiectelor ca fisiere pe disc si le citeste inapoi
@Service
public class ImageStorageService {

    // Numele fisierelor generate de noi: un UUID + extensie. Orice altceva este respins (protectie la "../" etc.)
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp|gif)$");

    private final Path uploadDir;

    public ImageStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

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

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Imaginea este obligatorie!");
        }

        ImageType type = detectType(file)
                .orElseThrow(() -> new IllegalArgumentException("Fisierul trebuie sa fie o imagine JPG, PNG, WebP sau GIF!"));

        String filename = UUID.randomUUID() + "." + type.extension;
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(uploadDir);
            Files.copy(in, uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Imaginea nu a putut fi salvata", e);
        }
        return filename;
    }

    // Calea fisierului, doar daca numele e valid si fisierul exista
    public Optional<Path> find(String filename) {
        if (filename == null || !FILENAME_PATTERN.matcher(filename).matches()) {
            return Optional.empty();
        }
        Path path = uploadDir.resolve(filename);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    public String contentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        return Arrays.stream(ImageType.values())
                .filter(t -> t.extension.equals(extension))
                .findFirst()
                .map(t -> t.contentType)
                .orElse("application/octet-stream");
    }

    public void delete(String filename) {
        find(filename).ifPresent(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Daca fisierul vechi nu poate fi sters nu blocam operatia
            }
        });
    }

    private Optional<ImageType> detectType(MultipartFile file) {
        byte[] header = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        } catch (IOException e) {
            return Optional.empty();
        }
        if (read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return Optional.of(ImageType.JPG);
        }
        if (read >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
            return Optional.of(ImageType.PNG);
        }
        if (read >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') {
            return Optional.of(ImageType.GIF);
        }
        if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return Optional.of(ImageType.WEBP);
        }
        return Optional.empty();
    }
}
