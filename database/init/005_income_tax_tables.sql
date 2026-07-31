CREATE TABLE IF NOT EXISTS income_tax_withholding_monthly_rates (
  no BIGSERIAL PRIMARY KEY,
  tax_year VARCHAR(4) NOT NULL,
  salary_type VARCHAR(20) NOT NULL,
  withholding_type VARCHAR(10) NOT NULL,
  dependent_count INTEGER,
  income_from NUMERIC(15, 2) NOT NULL,
  income_to NUMERIC(15, 2),
  tax_amount NUMERIC(15, 2) NOT NULL,
  base_tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
  additional_base_amount NUMERIC(15, 2),
  additional_rate NUMERIC(9, 5) NOT NULL DEFAULT 0,
  source_name VARCHAR(200) NOT NULL,
  remarks TEXT
);

CREATE INDEX IF NOT EXISTS idx_income_tax_withholding_monthly_lookup
  ON income_tax_withholding_monthly_rates (tax_year, salary_type, withholding_type, dependent_count, income_from, income_to);

CREATE INDEX IF NOT EXISTS idx_income_tax_withholding_monthly_income
  ON income_tax_withholding_monthly_rates (income_from, income_to);
