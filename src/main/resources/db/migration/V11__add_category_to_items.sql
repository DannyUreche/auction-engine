-- Categoria obiectului (valorile din enum-ul Category); obiectele existente intra la "Altele"
ALTER TABLE items
    ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'ALTELE';
