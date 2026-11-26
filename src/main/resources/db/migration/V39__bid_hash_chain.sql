-- Verifiable auctions: every bid is chained to the previous bid of the same item with SHA-256, so the bid
-- history can't be changed later without breaking the chain (browsers re-check it, see frontend/src/bidChain.js).
--
-- hash = sha256("id|prev_hash|item_id|bidder_id|amount|created_at|automatic")
--   amount     two decimals, e.g. 105.00
--   created_at 2026-11-25T18:04:05.123456 (always 6 decimals)
--   automatic  true / false
-- The first bid of an item has prev_hash = 64 zeros. Computed by a trigger, so every way of inserting a bid
-- (manual bid, auto-bid, buy now, accepted offer, demo data) follows the same rule.

ALTER TABLE bids ADD COLUMN prev_hash VARCHAR(64);
ALTER TABLE bids ADD COLUMN hash VARCHAR(64);

-- The chain is followed per item in id order
CREATE INDEX idx_bids_item_id_id ON bids (item_id, id);

CREATE FUNCTION bid_canonical(p_id INT, p_prev VARCHAR, p_item INT, p_bidder INT, p_amount NUMERIC,
                              p_created TIMESTAMP, p_automatic BOOLEAN) RETURNS TEXT AS
$$
SELECT p_id || '|' || p_prev || '|' || p_item || '|' || p_bidder || '|' || to_char(p_amount, 'FM999999999990.00') || '|'
           || to_char(p_created, 'YYYY-MM-DD"T"HH24:MI:SS.US') || '|' || CASE WHEN p_automatic THEN 'true' ELSE 'false' END
$$ LANGUAGE sql IMMUTABLE;

CREATE FUNCTION bid_hash(p_id INT, p_prev VARCHAR, p_item INT, p_bidder INT, p_amount NUMERIC,
                         p_created TIMESTAMP, p_automatic BOOLEAN) RETURNS VARCHAR AS
$$
SELECT encode(sha256(convert_to(bid_canonical(p_id, p_prev, p_item, p_bidder, p_amount, p_created, p_automatic), 'UTF8')), 'hex')
$$ LANGUAGE sql IMMUTABLE;

-- Existing bids: build the chains in id order
DO
$$
    DECLARE
        b         RECORD;
        last_item INT     := NULL;
        previous  VARCHAR := NULL;
    BEGIN
        UPDATE bids SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
        FOR b IN SELECT * FROM bids ORDER BY item_id, id
            LOOP
                IF last_item IS DISTINCT FROM b.item_id THEN
                    previous := repeat('0', 64);
                    last_item := b.item_id;
                END IF;
                UPDATE bids
                SET prev_hash = previous,
                    hash      = bid_hash(b.id, previous, b.item_id, b.bidder_id, b.amount, b.created_at, b.automatic)
                WHERE id = b.id
                RETURNING hash INTO previous;
            END LOOP;
    END
$$;

-- New bids: link to the latest bid of the item. Bids of one item are inserted one at a time
-- (the item row is locked while bidding), so "latest by id" is the previous link.
CREATE FUNCTION bids_chain() RETURNS TRIGGER AS
$$
DECLARE
    previous VARCHAR;
BEGIN
    IF NEW.created_at IS NULL THEN
        NEW.created_at := CURRENT_TIMESTAMP;
    END IF;
    SELECT hash INTO previous FROM bids WHERE item_id = NEW.item_id ORDER BY id DESC LIMIT 1;
    NEW.prev_hash := COALESCE(previous, repeat('0', 64));
    NEW.hash := bid_hash(NEW.id, NEW.prev_hash, NEW.item_id, NEW.bidder_id, NEW.amount, NEW.created_at, NEW.automatic);
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER bids_chain_before_insert
    BEFORE INSERT
    ON bids
    FOR EACH ROW
EXECUTE FUNCTION bids_chain();

ALTER TABLE bids ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE bids ALTER COLUMN prev_hash SET NOT NULL;
ALTER TABLE bids ALTER COLUMN hash SET NOT NULL;
