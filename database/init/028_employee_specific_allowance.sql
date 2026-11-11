ALTER TABLE employee_allowances
  ADD COLUMN IF NOT EXISTS specific_allowance NUMERIC(15, 2) NOT NULL DEFAULT 0;
