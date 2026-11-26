-- Orders with escrow: when an auction closes with a winner the price leaves the buyer's wallet but reaches the
-- seller only after the buyer confirms delivery (or automatically 7 days after shipping). A buyer can open a
-- dispute, which an admin settles (refund or release). Unshipped orders are refunded after 5 days.
CREATE TABLE orders
(
    id             SERIAL PRIMARY KEY,
    item_id        INT            NOT NULL UNIQUE REFERENCES items (id) ON DELETE CASCADE,
    buyer_id       INT            NOT NULL REFERENCES users (id),
    seller_id      INT            NOT NULL REFERENCES users (id),
    amount         DECIMAL(12, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    courier        VARCHAR(40),
    awb            VARCHAR(60),
    dispute_reason TEXT,
    resolution     TEXT,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    shipped_at     TIMESTAMP,
    disputed_at    TIMESTAMP,
    closed_at      TIMESTAMP
);

CREATE INDEX idx_orders_buyer ON orders (buyer_id);
CREATE INDEX idx_orders_seller ON orders (seller_id);
CREATE INDEX idx_orders_status ON orders (status);
