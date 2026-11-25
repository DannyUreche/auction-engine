-- Saved searches ("alerts"): the user is notified when a new auction matching them starts
CREATE TABLE saved_alerts
(
    id         SERIAL PRIMARY KEY,
    user_id    INT            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    query      VARCHAR(100),
    category   VARCHAR(30),
    min_price  DECIMAL(12, 2),
    max_price  DECIMAL(12, 2),
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_saved_alerts_user ON saved_alerts (user_id);
