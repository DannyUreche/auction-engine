-- Castigatorul licitatiei (NULL cat timp licitatia e activa sau daca nu a primit nicio oferta)
ALTER TABLE items
    ADD COLUMN winner_id INTEGER REFERENCES users(id) ON DELETE SET NULL;
