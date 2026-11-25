-- Audit log: every admin action (who, what, on which object, when)
CREATE TABLE admin_audit_log
(
    id           SERIAL PRIMARY KEY,
    admin        VARCHAR(50)  NOT NULL,
    action       VARCHAR(30)  NOT NULL,
    target_type  VARCHAR(20),
    target_id    INT,
    target_label VARCHAR(200),
    details      VARCHAR(300),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_audit_created ON admin_audit_log (created_at DESC);
