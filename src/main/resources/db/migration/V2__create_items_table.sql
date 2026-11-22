CREATE TABLE items (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       description TEXT,
                       start_price DECIMAL(10, 2) NOT NULL,
                       current_price DECIMAL(10, 2) NOT NULL,
                       seller_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE
);