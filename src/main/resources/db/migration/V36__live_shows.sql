-- Live show: a seller auctions several items one after another, each in its own time slot
CREATE TABLE live_shows
(
    id           SERIAL PRIMARY KEY,
    seller_id    INT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title        VARCHAR(100) NOT NULL,
    start_time   TIMESTAMP    NOT NULL,
    slot_seconds INT          NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE items ADD COLUMN live_show_id INT REFERENCES live_shows (id) ON DELETE SET NULL;
ALTER TABLE items ADD COLUMN show_position INT;

CREATE INDEX idx_items_live_show ON items (live_show_id);
