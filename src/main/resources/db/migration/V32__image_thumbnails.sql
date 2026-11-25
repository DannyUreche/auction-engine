-- Small JPEG copies of item photos for cards and lists (made on first request, see ThumbnailService)
CREATE TABLE image_thumbnails
(
    filename   VARCHAR(255) PRIMARY KEY REFERENCES item_images (filename) ON DELETE CASCADE,
    data       BYTEA     NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
