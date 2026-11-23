-- Imaginile se salveaza in baza de date (nu pe disc), ca sa nu se piarda pe serverele
-- unde fisierele se sterg la fiecare repornire (ex: Render, planul gratuit)
CREATE TABLE item_images (
    filename     VARCHAR(255) PRIMARY KEY,
    content_type VARCHAR(50)  NOT NULL,
    data         BYTEA        NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Imaginile vechi erau fisiere pe disc si nu pot fi copiate dintr-o migrare SQL:
-- obiectele raman fara imagine si vanzatorul o poate incarca din nou
UPDATE items SET image_filename = NULL;

ALTER TABLE items
    ADD CONSTRAINT fk_items_image FOREIGN KEY (image_filename) REFERENCES item_images (filename) ON DELETE SET NULL;
