CREATE TABLE IF NOT EXISTS employee_allowances (
  employee_no VARCHAR(20) NOT NULL,
  payment_month VARCHAR(7) NOT NULL,
  recruitment_allowance NUMERIC(15, 2) NOT NULL DEFAULT 0,
  PRIMARY KEY (employee_no, payment_month)
);

CREATE INDEX IF NOT EXISTS idx_employee_allowances_payment_month
  ON employee_allowances (payment_month);
