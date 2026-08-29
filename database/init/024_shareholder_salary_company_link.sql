ALTER TABLE shareholder_salaries
  ADD COLUMN IF NOT EXISTS company_total_manual BOOLEAN NOT NULL DEFAULT FALSE;
