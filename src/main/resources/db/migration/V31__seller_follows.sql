-- Following a seller: followers are notified when the seller lists a new auction
CREATE TABLE seller_follows
(
    id          SERIAL PRIMARY KEY,
    follower_id INT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    seller_id   INT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (follower_id, seller_id)
);

CREATE INDEX idx_seller_follows_seller ON seller_follows (seller_id);
