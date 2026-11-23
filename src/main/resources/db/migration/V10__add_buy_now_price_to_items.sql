-- Pret optional "Cumpara acum": cine il plateste castiga imediat licitatia
ALTER TABLE items
    ADD COLUMN buy_now_price DECIMAL(10, 2);
