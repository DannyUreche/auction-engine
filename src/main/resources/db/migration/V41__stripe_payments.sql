-- Card top-ups through Stripe Checkout (test mode unless live keys are configured). One row per paid
-- Checkout Session: the unique session id makes crediting idempotent (redirect and webhook may both arrive).
CREATE TABLE stripe_payments
(
    id         SERIAL PRIMARY KEY,
    session_id VARCHAR(255)   NOT NULL UNIQUE,
    user_id    INT            NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount     DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
