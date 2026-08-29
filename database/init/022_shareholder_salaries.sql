CREATE TABLE IF NOT EXISTS shareholder_salaries (
  employee_no VARCHAR(20) NOT NULL,
  payment_month VARCHAR(7) NOT NULL,
  contract_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
  overtime_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
  base_salary NUMERIC(15, 2) NOT NULL DEFAULT 0,
  advance_expense2 NUMERIC(15, 2) NOT NULL DEFAULT 0,
  company_total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
  PRIMARY KEY (employee_no, payment_month),
  CONSTRAINT fk_shareholder_salaries_employee
    FOREIGN KEY (employee_no) REFERENCES employee_masters(employee_no)
);

CREATE INDEX IF NOT EXISTS idx_shareholder_salaries_payment_month
  ON shareholder_salaries (payment_month);
