CREATE TABLE IF NOT EXISTS employee_masters (
  employee_no VARCHAR(20) PRIMARY KEY,
  employee_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(50),
  first_name VARCHAR(50),
  employee_type VARCHAR(50) NOT NULL,
  base_salary NUMERIC(15, 2) NOT NULL,
  dependent_count INTEGER NOT NULL,
  deduction_count INTEGER NOT NULL,
  consumption_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 0
);

ALTER TABLE employee_masters
  ADD COLUMN IF NOT EXISTS consumption_tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 0;

ALTER TABLE employee_masters
  ADD COLUMN IF NOT EXISTS deletion_flag VARCHAR(1) NOT NULL DEFAULT '0';

CREATE TABLE IF NOT EXISTS monthly_salary_calculation_conditions (
  employee_no VARCHAR(20) NOT NULL,
  payment_month VARCHAR(7) NOT NULL,
  base_salary NUMERIC(15, 2),
  commuting_allowance NUMERIC(15, 2),
  management_allowance NUMERIC(15, 2),
  transportation_adjustment NUMERIC(15, 2),
  overtime_pay NUMERIC(15, 2),
  advance_expense NUMERIC(15, 2),
  advance_expense2 NUMERIC(15, 2),
  other_amount NUMERIC(15, 2),
  other_item_name VARCHAR(100),
  PRIMARY KEY (employee_no, payment_month)
);

CREATE TABLE IF NOT EXISTS monthly_salary_details (
  employee_no VARCHAR(20) NOT NULL,
  payment_month VARCHAR(7) NOT NULL,
  employment_insurance_amount NUMERIC(15, 2),
  pension_insurance_amount NUMERIC(15, 2),
  health_insurance_amount NUMERIC(15, 2),
  income_tax NUMERIC(15, 2),
  child_support_amount NUMERIC(15, 2),
  taxable_income NUMERIC(15, 2),
  transfer_payment_amount NUMERIC(15, 2),
  company_employment_insurance_amount NUMERIC(15, 2),
  workers_accident_insurance_amount NUMERIC(15, 2),
  company_pension_insurance_amount NUMERIC(15, 2),
  company_health_insurance_amount NUMERIC(15, 2),
  company_total_amount NUMERIC(15, 2),
  PRIMARY KEY (employee_no, payment_month)
);

ALTER TABLE monthly_salary_details
  ADD COLUMN IF NOT EXISTS company_employment_insurance_amount NUMERIC(15, 2),
  ADD COLUMN IF NOT EXISTS workers_accident_insurance_amount NUMERIC(15, 2),
  ADD COLUMN IF NOT EXISTS company_pension_insurance_amount NUMERIC(15, 2),
  ADD COLUMN IF NOT EXISTS company_health_insurance_amount NUMERIC(15, 2),
  ADD COLUMN IF NOT EXISTS company_total_amount NUMERIC(15, 2);

CREATE TABLE IF NOT EXISTS employee_resident_tax_details (
  employee_no VARCHAR(20) NOT NULL,
  payment_month VARCHAR(7) NOT NULL,
  payment_amount NUMERIC(15, 2) NOT NULL,
  PRIMARY KEY (employee_no, payment_month)
);

CREATE INDEX IF NOT EXISTS idx_monthly_salary_conditions_payment_month
  ON monthly_salary_calculation_conditions (payment_month);

CREATE INDEX IF NOT EXISTS idx_monthly_salary_details_payment_month
  ON monthly_salary_details (payment_month);

CREATE INDEX IF NOT EXISTS idx_employee_resident_tax_details_payment_month
  ON employee_resident_tax_details (payment_month);
