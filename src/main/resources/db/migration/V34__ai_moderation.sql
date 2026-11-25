-- AI moderation: new listings are checked by Claude; suspicious ones become reports with source 'AI' (no reporter).
-- Existing items count as already checked.
ALTER TABLE items ADD COLUMN ai_checked BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE reports ALTER COLUMN reporter_id DROP NOT NULL;
ALTER TABLE reports ADD COLUMN source VARCHAR(10) NOT NULL DEFAULT 'USER';
