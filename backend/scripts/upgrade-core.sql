-- Manual PostgreSQL upgrade for existing installations. Run with psql -v ON_ERROR_STOP=1.
-- Check current records before altering types: do not silently truncate or round money.
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE users, transictions IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM users
        WHERE balance IS NULL OR outstanding_balance IS NULL
           OR balance < 0 OR outstanding_balance < 0
           OR balance > 9999999999999.99 OR outstanding_balance > 9999999999999.99
           OR balance <> round(balance, 2) OR outstanding_balance <> round(outstanding_balance, 2)
    ) THEN
        RAISE EXCEPTION 'Review invalid user balances before upgrading; no changes applied';
    END IF;
    IF EXISTS (SELECT 1 FROM users GROUP BY lower(email) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Resolve case-insensitive duplicate user emails before upgrading';
    END IF;
    IF EXISTS (SELECT 1 FROM users WHERE length(email) > 100 OR length(password) > 60) THEN
        RAISE EXCEPTION 'Review oversized user email/password fields before upgrading';
    END IF;
END $$;

ALTER TABLE users ALTER COLUMN balance TYPE numeric(15,2);
ALTER TABLE users ALTER COLUMN outstanding_balance TYPE numeric(15,2);
ALTER TABLE users ALTER COLUMN email TYPE varchar(100);
ALTER TABLE users ALTER COLUMN password TYPE varchar(60);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_lower ON users (lower(email));
CREATE INDEX IF NOT EXISTS idx_transactions_from_created ON transictions (from_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_to_created ON transictions (to_user_id, created_at DESC);

COMMIT;
