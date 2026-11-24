-- Scheduled auctions: start_time in the future (NULL = started when created).
-- start_notified: whether watchers were told that the auction started (TRUE for existing items).
ALTER TABLE items ADD COLUMN start_time TIMESTAMP;
ALTER TABLE items ADD COLUMN start_notified BOOLEAN NOT NULL DEFAULT TRUE;

-- Dutch auctions: the price starts high and drops by price_drop every minute, down to floor_price;
-- the first buyer wins at the current price.
ALTER TABLE items ADD COLUMN auction_type VARCHAR(10) NOT NULL DEFAULT 'ENGLISH';
ALTER TABLE items ADD COLUMN floor_price DECIMAL(10, 2);
ALTER TABLE items ADD COLUMN price_drop DECIMAL(10, 2);
