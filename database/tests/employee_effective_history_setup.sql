CREATE SCHEMA IF NOT EXISTS employee_history_verify;
SET search_path TO employee_history_verify, public;
-- Use the pre-migration snapshot so this test remains repeatable after deployment.
CREATE TABLE employee_masters (LIKE abms.employee_master_backup_before_history INCLUDING ALL);
ALTER TABLE employee_masters ADD PRIMARY KEY (employee_no);
INSERT INTO employee_masters SELECT * FROM abms.employee_master_backup_before_history;
CREATE TABLE off_book_expenses (
  handler_employee_no VARCHAR(20),
  CONSTRAINT fk_test_handler FOREIGN KEY (handler_employee_no) REFERENCES employee_masters(employee_no)
);
CREATE TABLE shareholder_salaries (
  employee_no VARCHAR(20),
  CONSTRAINT fk_test_shareholder FOREIGN KEY (employee_no) REFERENCES employee_masters(employee_no)
);
