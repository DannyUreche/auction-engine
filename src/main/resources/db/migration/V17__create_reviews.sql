-- Recenzii dupa vanzare: castigatorul si vanzatorul isi dau note (1-5 stele) unul altuia, o data pe obiect
CREATE TABLE reviews (
    id         SERIAL PRIMARY KEY,
    item_id    INT       NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    author_id  INT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_id  INT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rating     SMALLINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (item_id, author_id)
);

CREATE INDEX idx_reviews_target ON reviews (target_id);
