-- Web Push: the browsers/devices of a user that accept notifications
CREATE TABLE push_subscriptions (
    id         SERIAL PRIMARY KEY,
    user_id    INT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    endpoint   TEXT      NOT NULL UNIQUE,
    p256dh     TEXT      NOT NULL,
    auth       TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Small key-value store for server settings (the VAPID key pair of Web Push is generated once and kept here)
CREATE TABLE app_settings (
    key   VARCHAR(50) PRIMARY KEY,
    value TEXT        NOT NULL
);
