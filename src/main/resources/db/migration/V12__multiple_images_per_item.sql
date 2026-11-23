-- Un obiect poate avea mai multe poze (maxim 5, regula e in aplicatie).
-- "position" da ordinea afisarii: 0 = poza principala (cea de pe card)
CREATE TABLE item_photos (
    item_id        INT          NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    position       INT          NOT NULL,
    image_filename VARCHAR(255) NOT NULL REFERENCES item_images (filename),
    PRIMARY KEY (item_id, position)
);

-- Imaginea existenta a fiecarui obiect devine prima lui poza
INSERT INTO item_photos (item_id, position, image_filename)
SELECT id, 0, image_filename FROM items WHERE image_filename IS NOT NULL;

-- Coloana veche nu mai e folosita (se sterge si cheia straina fk_items_image)
ALTER TABLE items DROP COLUMN image_filename;
