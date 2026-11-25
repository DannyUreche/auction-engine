-- Error monitoring: unexpected server errors (HTTP 500) and JavaScript errors reported by browsers,
-- grouped by fingerprint (same source + message + first stack line = one row with a counter)
CREATE TABLE app_errors
(
    id          SERIAL PRIMARY KEY,
    fingerprint VARCHAR(64)  NOT NULL,
    source      VARCHAR(10)  NOT NULL,
    message     TEXT         NOT NULL,
    details     TEXT,
    path        VARCHAR(300),
    username    VARCHAR(50),
    user_agent  VARCHAR(300),
    occurrences INT          NOT NULL DEFAULT 1,
    first_seen  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved    BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_app_errors_open ON app_errors (resolved, fingerprint);
