-- Live auction room: public comments on an item and a view counter (unique visits, counted by LiveRoomService)
ALTER TABLE items ADD COLUMN view_count INT NOT NULL DEFAULT 0;

CREATE TABLE live_comments
(
    id         SERIAL PRIMARY KEY,
    item_id    INT          NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    author_id  INT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text       VARCHAR(300) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_live_comments_item ON live_comments (item_id, created_at);
