-- Notificarile fiecarui utilizator (clopotelul din bara de sus): oferta depasita, oferta noua la obiectul tau,
-- licitatie castigata, obiect vandut / nevandut
CREATE TABLE notifications (
    id         SERIAL PRIMARY KEY,
    user_id    INT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(20) NOT NULL,
    message    TEXT        NOT NULL,
    item_id    INT         REFERENCES items (id) ON DELETE CASCADE,
    read       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);
