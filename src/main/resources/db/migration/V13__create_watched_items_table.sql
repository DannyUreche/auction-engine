-- Obiectele urmarite (favorite) de fiecare utilizator
CREATE TABLE watched_items (
    id         SERIAL PRIMARY KEY,
    user_id    INT       NOT NULL REFERENCES users (id),
    item_id    INT       NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- un utilizator urmareste un obiect o singura data
    UNIQUE (user_id, item_id)
);
