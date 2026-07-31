CREATE TABLE IF NOT EXISTS employment_insurance_rates (
  no BIGSERIAL PRIMARY KEY,
  fiscal_year VARCHAR(20) NOT NULL,
  effective_start_date DATE NOT NULL,
  effective_end_date DATE NOT NULL,
  business_category VARCHAR(100) NOT NULL,
  worker_rate NUMERIC(7, 4) NOT NULL,
  employer_rate NUMERIC(7, 4) NOT NULL,
  total_rate NUMERIC(7, 4) NOT NULL,
  remarks TEXT
);

CREATE INDEX IF NOT EXISTS idx_employment_insurance_rates_period
  ON employment_insurance_rates (effective_start_date, effective_end_date);

CREATE INDEX IF NOT EXISTS idx_employment_insurance_rates_category
  ON employment_insurance_rates (business_category);

CREATE TABLE IF NOT EXISTS social_insurance_premium_amounts (
  no BIGSERIAL PRIMARY KEY,
  effective_month VARCHAR(7) NOT NULL,
  payment_month VARCHAR(7) NOT NULL,
  prefecture VARCHAR(20) NOT NULL,
  grade INTEGER NOT NULL,
  pension_grade INTEGER,
  standard_monthly_remuneration INTEGER NOT NULL,
  monthly_remuneration_from INTEGER,
  monthly_remuneration_to INTEGER,
  health_insurance_rate NUMERIC(7, 4),
  health_insurance_total_amount NUMERIC(15, 2),
  health_insurance_employee_amount NUMERIC(15, 2),
  health_insurance_employer_amount NUMERIC(15, 2),
  care_insurance_rate NUMERIC(7, 4),
  care_insurance_total_amount NUMERIC(15, 2),
  care_insurance_employee_amount NUMERIC(15, 2),
  care_insurance_employer_amount NUMERIC(15, 2),
  child_support_rate NUMERIC(7, 4),
  child_support_total_amount NUMERIC(15, 2),
  child_support_employee_amount NUMERIC(15, 2),
  child_support_employer_amount NUMERIC(15, 2),
  pension_insurance_rate NUMERIC(7, 4),
  pension_insurance_total_amount NUMERIC(15, 2),
  pension_insurance_employee_amount NUMERIC(15, 2),
  pension_insurance_employer_amount NUMERIC(15, 2),
  source_name VARCHAR(200),
  source_date DATE,
  remarks TEXT
);

CREATE INDEX IF NOT EXISTS idx_social_insurance_premium_amounts_month
  ON social_insurance_premium_amounts (effective_month, payment_month);

CREATE INDEX IF NOT EXISTS idx_social_insurance_premium_amounts_prefecture
  ON social_insurance_premium_amounts (prefecture);

CREATE INDEX IF NOT EXISTS idx_social_insurance_premium_amounts_grade
  ON social_insurance_premium_amounts (grade);

CREATE INDEX IF NOT EXISTS idx_social_insurance_premium_amounts_pension_grade
  ON social_insurance_premium_amounts (pension_grade);
