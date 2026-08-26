BEGIN;

-- Reassign the current 0029 employee master record to employee number 0014.
UPDATE employee_masters
SET employee_no = '0014'
WHERE employee_no = '0029'
  AND NOT EXISTS (SELECT 1 FROM employee_masters WHERE employee_no = '0014');

-- Register the existing 0004 payroll-detail employee as 呂　加舜.
INSERT INTO employee_masters (
  employee_no,
  employee_name,
  last_name,
  first_name,
  employee_type,
  base_salary,
  dependent_count,
  deduction_count,
  consumption_tax_rate,
  deletion_flag
)
VALUES (
  '0004',
  '呂　加舜',
  '呂',
  '加舜',
  '1',
  400000,
  0,
  0,
  0.08,
  '1'
)
ON CONFLICT (employee_no) DO UPDATE SET
  employee_name = EXCLUDED.employee_name,
  last_name = EXCLUDED.last_name,
  first_name = EXCLUDED.first_name,
  employee_type = EXCLUDED.employee_type,
  base_salary = EXCLUDED.base_salary,
  consumption_tax_rate = EXCLUDED.consumption_tax_rate,
  deletion_flag = EXCLUDED.deletion_flag;

COMMIT;
