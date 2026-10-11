-- Manual PostgreSQL upgrade for an existing TrustPay schema.
-- Stop the application, back up the database, then run:
-- psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f backend/scripts/upgrade-loans.sql
-- DATABASE_URL here is a PostgreSQL connection URI, not the application's JDBC URL.
-- Never round, delete, or renumber legacy financial records automatically.

BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE loans, loan_installments, companies IN ACCESS EXCLUSIVE MODE;

DO $$
DECLARE
    inconsistent_schedules bigint;
BEGIN
    IF EXISTS (
        SELECT 1 FROM loans
        WHERE principal_amount IS NULL OR total_amount IS NULL
           OR principal_amount <= 0 OR total_amount <= 0
           OR principal_amount > 9999999999999.99 OR total_amount > 9999999999999.99
           OR principal_amount <> round(principal_amount, 2)
           OR total_amount <> round(total_amount, 2)
    ) THEN
        RAISE EXCEPTION 'Loan amounts contain null, nonpositive, overflowing, or sub-cent values; review them before this upgrade';
    END IF;

    IF EXISTS (
        SELECT 1 FROM loans
        WHERE user_id IS NULL OR company_id IS NULL OR total_installments IS NULL
           OR status IS NULL OR created_at IS NULL
    ) THEN
        RAISE EXCEPTION 'Loans contain missing required values; review them before this upgrade';
    END IF;

    IF EXISTS (
        SELECT 1 FROM loan_installments
        WHERE amount IS NULL OR amount <= 0 OR amount > 9999999999999.99
           OR amount <> round(amount, 2)
           OR loan_id IS NULL OR installment_number IS NULL
           OR due_date IS NULL OR status IS NULL
    ) THEN
        RAISE EXCEPTION 'Installments contain invalid amounts or missing required values; review them before this upgrade';
    END IF;

    IF EXISTS (
        SELECT 1 FROM loan_installments
        GROUP BY loan_id, installment_number
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate installment numbers exist for a loan; review them before adding the unique constraint';
    END IF;

    IF EXISTS (
        SELECT 1 FROM companies
        WHERE name IS NULL OR email IS NULL OR btrim(name) = '' OR btrim(email) = ''
           OR length(name) > 100 OR length(email) > 100
    ) THEN
        RAISE EXCEPTION 'Partner companies contain missing or oversized name/email values; review them before this upgrade';
    END IF;

    SELECT count(*) INTO inconsistent_schedules
    FROM loans loan
    LEFT JOIN (
        SELECT loan_id, sum(amount) AS scheduled_total, count(*) AS scheduled_count
        FROM loan_installments GROUP BY loan_id
    ) schedule ON schedule.loan_id = loan.id
    WHERE loan.status = 'ACTIVE'
      AND (schedule.scheduled_total IS DISTINCT FROM loan.total_amount
           OR schedule.scheduled_count IS DISTINCT FROM loan.total_installments::bigint);
    IF inconsistent_schedules > 0 THEN
        RAISE NOTICE '% active loans have schedules that differ from their total/count; review these records and account outstanding balances before allowing repayment', inconsistent_schedules;
    END IF;
END $$;

ALTER TABLE loans
    ALTER COLUMN principal_amount TYPE numeric(15, 2),
    ALTER COLUMN total_amount TYPE numeric(15, 2),
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN company_id SET NOT NULL,
    ALTER COLUMN principal_amount SET NOT NULL,
    ALTER COLUMN total_amount SET NOT NULL,
    ALTER COLUMN total_installments SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE loan_installments
    ALTER COLUMN amount TYPE numeric(15, 2),
    ALTER COLUMN loan_id SET NOT NULL,
    ALTER COLUMN installment_number SET NOT NULL,
    ALTER COLUMN amount SET NOT NULL,
    ALTER COLUMN due_date SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE companies
    ALTER COLUMN name TYPE varchar(100),
    ALTER COLUMN email TYPE varchar(100),
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN email SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_loans_user_status ON loans (user_id, status);
CREATE INDEX IF NOT EXISTS idx_installments_loan_status ON loan_installments (loan_id, status);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'loan_installments'::regclass
          AND conname = 'uk_loan_installment_number'
    ) THEN
        ALTER TABLE loan_installments
            ADD CONSTRAINT uk_loan_installment_number UNIQUE (loan_id, installment_number);
    END IF;
END $$;

COMMIT;

-- Inspect legacy schedules that were not reconciled by this schema-only upgrade:
-- SELECT loan.id, loan.total_amount, sum(installment.amount) AS scheduled_total,
--        count(installment.id) AS scheduled_count, loan.total_installments
-- FROM loans loan LEFT JOIN loan_installments installment ON installment.loan_id = loan.id
-- WHERE loan.status = 'ACTIVE'
-- GROUP BY loan.id
-- HAVING sum(installment.amount) IS DISTINCT FROM loan.total_amount
--     OR count(installment.id) IS DISTINCT FROM loan.total_installments::bigint;
