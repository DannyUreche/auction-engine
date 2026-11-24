-- Reports of suspicious items (moderated by admins); one report per user and item
CREATE TABLE reports (
    id          SERIAL PRIMARY KEY,
    item_id     INT         NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    reporter_id INT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reason      VARCHAR(20) NOT NULL,
    details     TEXT,
    status      VARCHAR(12) NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    UNIQUE (item_id, reporter_id)
);

-- Blocked accounts cannot log in and their tokens stop working
ALTER TABLE users ADD COLUMN blocked BOOLEAN NOT NULL DEFAULT FALSE;
