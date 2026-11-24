-- Link-uri de resetare a parolei. Se salveaza doar amprenta SHA-256 a token-ului (nu token-ul),
-- deci cine vede baza de date nu poate folosi link-urile.
CREATE TABLE password_reset_tokens (
    id         SERIAL PRIMARY KEY,
    user_id    INT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP   NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE
);
