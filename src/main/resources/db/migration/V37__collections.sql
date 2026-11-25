-- Personal lists of auctions ("colectii"), public by default so they can be shared with a link
CREATE TABLE collections
(
    id          SERIAL PRIMARY KEY,
    owner_id    INT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(80)  NOT NULL,
    description VARCHAR(500),
    is_public   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_collections_owner ON collections (owner_id);

CREATE TABLE collection_items
(
    id            SERIAL PRIMARY KEY,
    collection_id INT       NOT NULL REFERENCES collections (id) ON DELETE CASCADE,
    item_id       INT       NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    added_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (collection_id, item_id)
);

CREATE INDEX idx_collection_items_item ON collection_items (item_id);
