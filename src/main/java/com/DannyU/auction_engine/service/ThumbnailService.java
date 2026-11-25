package com.DannyU.auction_engine.service;

import com.DannyU.auction_engine.entity.ImageThumbnail;
import com.DannyU.auction_engine.entity.ItemImage;
import com.DannyU.auction_engine.repository.ImageThumbnailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

// Thumbnails for cards and lists: a 480px-wide JPEG made the first time it is asked for and stored, so a page
// with 12 cards downloads ~30 KB per photo instead of the full uploads (up to 5 MB each).
@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    static final int WIDTH = 480;
    static final float QUALITY = 0.78f;

    private final ImageThumbnailRepository thumbnailRepository;
    private final ImageStorageService imageStorageService;

    // Thumbnail bytes (JPEG) or empty when the photo doesn't exist. Falls back to nothing if the format can't be
    // decoded (e.g. WebP without a decoder) - the controller then serves the original.
    @Transactional
    public Optional<byte[]> thumbnail(String filename) {
        Optional<ImageThumbnail> stored = thumbnailRepository.findById(filename);
        if (stored.isPresent()) {
            return Optional.of(stored.get().getData());
        }
        Optional<ItemImage> original = imageStorageService.find(filename);
        if (original.isEmpty()) {
            return Optional.empty();
        }
        byte[] data = resize(original.get().getData());
        if (data == null) {
            return Optional.empty();
        }
        ImageThumbnail thumbnail = new ImageThumbnail();
        thumbnail.setFilename(filename);
        thumbnail.setData(data);
        thumbnailRepository.save(thumbnail);
        return Optional.of(data);
    }

    static byte[] resize(byte[] original) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                return null;
            }
            int width = Math.min(WIDTH, source.getWidth());
            int height = Math.max(1, Math.round(source.getHeight() * (width / (float) source.getWidth())));
            BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(java.awt.Color.WHITE); // transparent PNGs get a white background
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, width, height, null);
            g.dispose();

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(QUALITY);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(stream);
                writer.write(null, new IIOImage(target, null, null), params);
            } finally {
                writer.dispose();
            }
            return out.toByteArray();
        } catch (IOException | RuntimeException e) {
            log.warn("Could not make a thumbnail: {}", e.getMessage());
            return null;
        }
    }
}
