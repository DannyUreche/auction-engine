-- Licitare automata: utilizatorul spune suma maxima, iar aplicatia liciteaza pentru el cu pasul minim
-- (maximul ramane secret; vezi BidService.resolveAutoBids)
CREATE TABLE auto_bids (
    id         SERIAL PRIMARY KEY,
    item_id    INT            NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    user_id    INT            NOT NULL REFERENCES users (id),
    max_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (item_id, user_id)
);

-- Ofertele plasate automat sunt marcate (in istoric apar cu eticheta "automat")
ALTER TABLE bids ADD COLUMN automatic BOOLEAN NOT NULL DEFAULT FALSE;
