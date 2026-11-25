-- Two-factor authentication (TOTP, e.g. Google Authenticator). The secret is set when the user starts the
-- setup; two_factor_enabled becomes true only after a correct code. Backup codes are stored hashed.
ALTER TABLE users ADD COLUMN two_factor_secret VARCHAR(64);
ALTER TABLE users ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE two_factor_backup_codes
(
    id        SERIAL PRIMARY KEY,
    user_id   INT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash VARCHAR(64) NOT NULL,
    used      BOOLEAN     NOT NULL DEFAULT FALSE
);
