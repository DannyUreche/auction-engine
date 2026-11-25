-- Read receipts in the buyer-seller chat: when the recipient saw the message (NULL = not yet)
ALTER TABLE messages ADD COLUMN read_at TIMESTAMP;
UPDATE messages SET read_at = created_at;
