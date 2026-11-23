-- Numele fisierului imaginii obiectului (fisierul este salvat in folderul de upload-uri)
ALTER TABLE items
    ADD COLUMN image_filename VARCHAR(255);
