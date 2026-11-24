-- Chat intre castigator si vanzator dupa vanzare (pe pagina obiectului; doar ei doi il pot citi)
CREATE TABLE messages (
    id         SERIAL PRIMARY KEY,
    item_id    INT       NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    sender_id  INT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text       TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_item ON messages (item_id, created_at);
