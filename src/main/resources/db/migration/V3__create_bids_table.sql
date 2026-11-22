CREATE TABLE bids (
                      id SERIAL PRIMARY KEY,
                      item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
                      bidder_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                      amount DECIMAL(10, 2) NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);