-- Public questions about an item, answered by the seller
CREATE TABLE questions (
    id          SERIAL PRIMARY KEY,
    item_id     INT       NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    author_id   INT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text        TEXT      NOT NULL,
    answer      TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answered_at TIMESTAMP
);

CREATE INDEX idx_questions_item ON questions (item_id, created_at);
