BEGIN;
CREATE TEMP TABLE history_assert(ok BOOLEAN CHECK(ok)) ON COMMIT DROP;
INSERT INTO history_assert SELECT
  (SELECT COUNT(*) FROM employee_masters) = (SELECT COUNT(*) FROM employee_master_backup_before_history)
  AND NOT EXISTS (SELECT 1 FROM employee_masters WHERE effective_start_date <> DATE '1900-01-01'
                  OR effective_end_date <> DATE '9999-12-31');
INSERT INTO employee_masters(employee_no,employee_name,last_name,first_name,employee_type,
  base_salary,dependent_count,deduction_count,effective_start_date,effective_end_date)
VALUES
('__HISTORY_TEST__','History Test','History','Test','0',100000,0,0,'2025-10-01','2025-12-31'),
('__HISTORY_TEST__','History Test','History','Test','0',200000,1,1,'2026-01-01','2026-03-14'),
('__HISTORY_TEST__','History Test','History','Test','0',300000,2,2,'2026-03-15','2098-12-31'),
('__HISTORY_TEST__','History Test','History','Test','0',400000,3,3,'2099-01-01','9999-12-31');
INSERT INTO history_assert SELECT (SELECT base_salary FROM employee_masters_for_month('202512') WHERE employee_no='__HISTORY_TEST__')=100000;
INSERT INTO history_assert SELECT (SELECT base_salary FROM employee_masters_for_month('202601') WHERE employee_no='__HISTORY_TEST__')=200000;
INSERT INTO history_assert SELECT (SELECT base_salary FROM employee_masters_for_month('202603') WHERE employee_no='__HISTORY_TEST__')=300000;
INSERT INTO history_assert SELECT (SELECT base_salary FROM employee_masters_for_month('209901') WHERE employee_no='__HISTORY_TEST__')=400000;
INSERT INTO history_assert SELECT (SELECT COUNT(*) FROM current_employee_masters WHERE employee_no='__HISTORY_TEST__')=1
  AND (SELECT base_salary FROM current_employee_masters WHERE employee_no='__HISTORY_TEST__')=300000;
INSERT INTO history_assert SELECT (SELECT base_salary FROM employee_masters_for_period('2025-12-31','2026-01-01') WHERE employee_no='__HISTORY_TEST__')=200000;
INSERT INTO history_assert SELECT NOT EXISTS (SELECT 1 FROM employee_masters_for_period('2025-09-01','2025-09-30') WHERE employee_no='__HISTORY_TEST__');
INSERT INTO off_book_expenses VALUES('__HISTORY_TEST__');
INSERT INTO shareholder_salaries VALUES('__HISTORY_TEST__');
INSERT INTO history_assert SELECT (SELECT COUNT(*) FROM pg_constraint
 WHERE contype='f' AND connamespace='employee_history_verify'::regnamespace
 AND confrelid='employee_numbers'::regclass)=3;
SELECT COUNT(*) AS passed_sql_checks FROM history_assert;
COMMIT;
SET search_path TO public;
DROP SCHEMA employee_history_verify CASCADE;
