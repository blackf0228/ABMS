ALTER TABLE monthly_salary_details
  ADD COLUMN IF NOT EXISTS company_child_support_amount NUMERIC(15, 2) NOT NULL DEFAULT 0;
