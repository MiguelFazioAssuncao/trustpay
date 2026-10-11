-- Manual PostgreSQL upgrade for an existing TrustPay schema, with the application stopped.
-- Back up first and run with psql -v ON_ERROR_STOP=1. No historical prices/quantities are invented.
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE orders, product, card IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM product
        WHERE name IS NULL OR btrim(name) = '' OR length(name) > 120
           OR price IS NULL OR price <= 0 OR price > 9999999999999.99
           OR price <> round(price, 2) OR stock IS NULL OR stock < 0
    ) THEN
        RAISE EXCEPTION 'Products contain invalid names, prices, or stock; review them before upgrading';
    END IF;
    IF EXISTS (
        SELECT 1 FROM card
        WHERE card_number IS NULL OR btrim(card_number) = '' OR user_id IS NULL
           OR type IS NULL OR type NOT IN ('DEBIT_CARD', 'CREDIT_CARD')
           OR card_limit IS NULL OR card_limit < 0 OR card_limit > 9999999999999.99
           OR card_limit <> round(card_limit, 2)
           OR (type = 'CREDIT_CARD' AND card_limit = 0)
    ) THEN
        RAISE EXCEPTION 'Cards contain invalid numbers, owners, types, or limits; review them before upgrading';
    END IF;
    IF EXISTS (
        SELECT 1 FROM orders
        WHERE user_id IS NULL OR total_amount IS NULL OR total_amount <= 0
           OR total_amount > 9999999999999.99 OR total_amount <> round(total_amount, 2)
           OR status IS NULL OR created_at IS NULL
    ) THEN
        RAISE EXCEPTION 'Orders contain invalid totals or missing required values; review them before upgrading';
    END IF;
END $$;

ALTER TABLE product
    ALTER COLUMN name TYPE varchar(120),
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN price TYPE numeric(15,2),
    ALTER COLUMN price SET NOT NULL,
    ALTER COLUMN stock SET NOT NULL;
ALTER TABLE card
    ALTER COLUMN card_number SET NOT NULL,
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN card_limit TYPE numeric(15,2),
    ALTER COLUMN card_limit SET NOT NULL;
ALTER TABLE orders ALTER COLUMN total_amount TYPE numeric(15,2);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method varchar(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS card_id uuid;
ALTER TABLE product ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE card ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE card ADD COLUMN IF NOT EXISTS used_limit numeric(15,2) NOT NULL DEFAULT 0;
-- Initialize only new/previously uninitialized tracking metadata.
UPDATE product SET version = 0 WHERE version IS NULL;
UPDATE card SET version = 0 WHERE version IS NULL;
UPDATE card SET used_limit = 0 WHERE used_limit IS NULL;
ALTER TABLE product ALTER COLUMN version SET DEFAULT 0, ALTER COLUMN version SET NOT NULL;
ALTER TABLE card
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL,
    ALTER COLUMN used_limit SET DEFAULT 0,
    ALTER COLUMN used_limit SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM card
        WHERE used_limit < 0 OR used_limit > card_limit OR used_limit <> round(used_limit, 2)
    ) THEN
        RAISE EXCEPTION 'Existing card usage exceeds its limit or contains invalid cents; review it before upgrading';
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS order_items (
    id uuid PRIMARY KEY,
    order_id uuid NOT NULL REFERENCES orders(id),
    product_id uuid NOT NULL,
    product_name varchar(120) NOT NULL,
    quantity integer NOT NULL CHECK (quantity > 0),
    unit_price numeric(15,2) NOT NULL CHECK (unit_price > 0),
    CONSTRAINT uk_order_item_product UNIQUE (order_id, product_id)
);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_created ON orders(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_card_user_id ON card(user_id);

COMMIT;

-- Original orders never recorded quantities or prices per item. Do not invent them.
-- Their existing orders_products associations remain readable; items stays empty.
-- This script does not alter existing card numbers. New cards only store last four digits.
-- Review/remediate existing full card numbers separately before using legacy data.
