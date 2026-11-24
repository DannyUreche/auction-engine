-- Virtual wallet: every account starts with 10 000 RON of test credits. The amount of every auction you
-- are currently leading is reserved; money moves from the winner to the seller when an auction closes.
ALTER TABLE users ADD COLUMN balance DECIMAL(12, 2) NOT NULL DEFAULT 10000;

CREATE TABLE wallet_transactions (
    id          SERIAL PRIMARY KEY,
    user_id     INT            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(20)    NOT NULL,
    amount      DECIMAL(12, 2) NOT NULL,
    item_id     INT            REFERENCES items (id) ON DELETE SET NULL,
    description TEXT           NOT NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wallet_transactions_user ON wallet_transactions (user_id, created_at DESC);
