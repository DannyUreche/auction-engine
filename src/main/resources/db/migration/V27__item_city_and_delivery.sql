-- Where the item is and how it reaches the buyer (filter by city on the home page)
ALTER TABLE items ADD COLUMN city VARCHAR(60);
ALTER TABLE items ADD COLUMN delivery VARCHAR(20) NOT NULL DEFAULT 'BOTH';

CREATE INDEX idx_items_city ON items (LOWER(city));
