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
  '0019',
  '孔　令豪',
  '孔',
  '令豪',
  '0',
  230000,
  0,
  0,
  0,
  '1'
)
ON CONFLICT (employee_no) DO UPDATE SET
  employee_name = EXCLUDED.employee_name,
  last_name = EXCLUDED.last_name,
  first_name = EXCLUDED.first_name,
  employee_type = EXCLUDED.employee_type,
  base_salary = EXCLUDED.base_salary,
  deletion_flag = EXCLUDED.deletion_flag;
