-- Private offers ("Fa o oferta"): a buyer proposes a price before anyone bids; the seller accepts, rejects or
-- counters, and an accepted offer sells the item at that price
CREATE TABLE offers
(
    id             SERIAL PRIMARY KEY,
    item_id        INT            NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    buyer_id       INT            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount         DECIMAL(12, 2) NOT NULL,
    message        VARCHAR(300),
    status         VARCHAR(12)    NOT NULL DEFAULT 'PENDING',
    counter_amount DECIMAL(12, 2),
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at   TIMESTAMP
);

CREATE INDEX idx_offers_item ON offers (item_id, status);
